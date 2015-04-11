/*
 * Copyright (C) 2013 Samuel Audet
 *
 * This file is part of JavaCPP.
 *
 * JavaCPP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version (subject to the "Classpath" exception
 * as provided in the LICENSE.txt file that accompanied this code).
 *
 * JavaCPP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JavaCPP.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.googlecode.javacpp;

import java.io.*;
import java.util.*;

public class Parser {
    public Parser(Reader input, Writer output) {
        this.input = input;
        this.output = output;
    }

    class Token {
        static final int
                NUMBER     = 1,
                STRING     = 2,
                IDENTIFIER = 3,
                COMMENT    = 4;

        int type;
        String spacing, value;
    }

    class TokenList extends LinkedList<Token> {
        ListIterator<Token> iterator = listIterator();
        Token getOrAdd(Token t) {
            if (iterator.hasNext()) {
                return iterator.next();
            }
            if (t == null) {
                t = new Token();
            }
            iterator.add(t);
            return t;
        }
        void reset() {
            iterator = listIterator();
        }
        @Override public void clear() {
            super.clear();
            reset();
        }
    }

    Reader input;
    Writer output, out;
    int lastc = -1, lineNumber = 0;
    StringBuilder buffer = new StringBuilder();
    String lineSeparator = System.getProperty("line.separator");

    Token token = null, lastToken = null;
    TokenList pastTokens = new TokenList();
    TokenList futureTokens = new TokenList();

    Token nextToken() throws IOException {
        if (lastToken != null) {
            token = lastToken;
            lastToken = null;
            return token;
        }
        token = new Token();
        int c = lastc == -1 ? input.read() : lastc;
        lastc = -1;

        buffer.setLength(0);
        if (Character.isWhitespace(c)) {
            buffer.append((char)c);
            if (c == '\r' || c == '\n') {
                lineNumber++;
            }
            while ((c = input.read()) != -1 && Character.isWhitespace(c)) {
                buffer.append((char)c);
                if (c == '\r' || c == '\n') {
                    lineNumber++;
                }
            }
        }
        token.spacing = buffer.toString();

        buffer.setLength(0);
        if (Character.isLetter(c) || c == '_') {
            token.type = Token.IDENTIFIER;
            buffer.append((char)c);
            while ((c = input.read()) != -1 && (Character.isLetter(c) || Character.isDigit(c) || c == '_')) {
                buffer.append((char)c);
            }
            token.value = buffer.toString();
            lastc = c;
        } else if (Character.isDigit(c) || c == '-' || c == '.') {
            token.type = Token.NUMBER;
            buffer.append((char)c);
            while ((c = input.read()) != -1 && (Character.isDigit(c) || c == '.')) {
                buffer.append((char)c);
            }
            token.value = buffer.toString();
            lastc = c;
        } else if (c == '/') {
            c = input.read();
            if (c == '/') {
                token.type = Token.COMMENT;
                buffer.append('/').append('/');
                while ((c = input.read()) != -1 && c != '\r' && c != '\n') {
                    buffer.append((char)c);
                }
                token.value = buffer.toString();
                lastc = c;
            } else if (c == '*') {
                token.type = Token.COMMENT;
                buffer.append('/').append('*');
                int prevc = 0;
                while ((c = input.read()) != -1 && (prevc != '*' || c != '/')) {
                    buffer.append((char)c);
                    prevc = c;
                }
                buffer.append((char)c);
                token.value = buffer.toString();
            } else {
                lastc = c;
                token.type = '/';
            }
        } else {
            token.type = c;
        }
        return token;
    }

    void match(int tokenType) {
        if (token.type != tokenType) {
            throw new RuntimeException(lineNumber + ": Unexpected token '" +
                    (token.value != null && token.value.length() > 0 ?
                    token.value : (char)token.type) + "'");
        }
    }

    String modifiers() {
        String s = "";
        int mod1 = pastTokens.size() > 1 ? pastTokens.get(1).type : -1;
        int mod2 = pastTokens.size() > 2 ? pastTokens.get(2).type : -1;

        if (mod1 == '&') {
            s = "@ByRef ";
        } else if (mod1 == '*' && mod2 == '&') {
            s = "@ByPtrRef ";
        } else if (mod1 == '*' && mod2 == '*') {
            s = "@ByPtrPtr ";
        } else if (mod1 != '*') {
            s = "@ByVal ";
        }
        pastTokens.clear();
        return s;
    }

    Token commentBefore() throws IOException {
        Token type = null;
        for (Token t : pastTokens) {
            if (t.type == Token.IDENTIFIER) {
                type = t;
                break;
            } else if (t.type == Token.COMMENT) {
                out.append(t.spacing + t.value);
            }
        }
        return type;
    }

    void commentAfter(Token comment) throws IOException {
        comment.spacing = "";
        comment.value = "";
        while (nextToken().type == Token.COMMENT && token.value.charAt(3) == '<') {
            String spacing = token.spacing;
            String value = token.value;
            if (value.charAt(1) == '/') {
                comment.value += (comment.value.length() > 0 ? " * " : "/**") + value.substring(4);
            } else {
                comment.value += spacing + value;
            }
        }
        if (comment.value.length() > 0) {
            if (!comment.value.endsWith("*/")) {
                comment.value += " */";
            }
            comment.value += lineSeparator;
        }
        lastToken = token;
    }

    void function() throws IOException {
        match('(');

        Token type = commentBefore();
        String name = pastTokens.getLast().value;
        pastTokens.clear();
        String spacing = type.spacing;
        Token comment = futureTokens.getOrAdd(null);
        if (comment.value != null && comment.value.length() > 0) {
            out.append(type.spacing + comment.value);
            spacing = type.spacing.substring(type.spacing.lastIndexOf('\n') + 1);
        }
        out.append(spacing + "public static " + modifiers() + type.value + " " + name + "(");

        boolean done = false;
        while (!done && nextToken().type != -1) {
            switch (token.type) {
                case ')':
                case ',':
                    if (pastTokens.size() > 0) {
                        spacing = pastTokens.getFirst().spacing;
                        type = pastTokens.getFirst();
                        name = pastTokens.getLast().value;
                        out.append(spacing + modifiers() + type.value + " " + name);
                    }
                    out.append((char)token.type);
                    break;
                case ';': done = true; break;
                default: pastTokens.add(token); break;
            }
        }
        match(';');
        out.append(token.spacing + ';');
        commentAfter(comment);
    }

    void variable(Token group) throws IOException {
        match(';');

        Token type = commentBefore();
        String name = pastTokens.getLast().value;
        pastTokens.clear();
        String spacing = type.spacing;
        Token comment = futureTokens.getOrAdd(null);
        if (comment.value != null && comment.value.length() > 0) {
            out.append(type.spacing + comment.value);
            spacing = type.spacing.substring(type.spacing.lastIndexOf('\n') + 1);
        }
        String access = "public ";
        String setterType = "void ";
        if (group != null) {
            setterType = group.value + " ";
        } else {
            access += "static ";
        }
        out.append(spacing + modifiers() + access + type.value + " " + name + "(); ");
        out.append(access + setterType + name + "(" + type.value + " " + name + ");");
        out.append(token.spacing);
        commentAfter(comment);
    }

    void macro() throws IOException {
        match('#');
        String spacing = token.spacing;

        while (nextToken().type != -1) {
            int c = token.spacing.length() > 0 ? token.spacing.charAt(0) : -1;
            if (c == '\r' || c == '\n') {
                if (pastTokens.getLast().type == '\\') {
                    pastTokens.pop();
                } else {
                    break;
                }
            }
            pastTokens.add(token);
        }
        Token keyword = pastTokens.size() > 0 ? pastTokens.getFirst() : null;
        if (keyword == null || keyword.type != Token.IDENTIFIER) {
            throw new RuntimeException(lineNumber + ": Could not parse macro");
        }
        if (keyword.value.equals("define") && pastTokens.size() > 2) {
            Token name  = pastTokens.get(1);
            Token value = pastTokens.get(2);
            String type;
            switch (value.type) {
                case Token.NUMBER: type = "int ";    break;
                case Token.STRING: type = "String "; break;
                default: return;
            }
            out.append(spacing + "public static final " + type);
            out.append(name.value + " = " + value.value + ";");
        }
        out.append(token.spacing);
    }

    void group() throws IOException {
        boolean block = pastTokens.size() > 0;
        Token name = null;
        if (block) {
            match('{');

            Token type = commentBefore();
            name = pastTokens.getLast();
            pastTokens.clear();
            name = futureTokens.getOrAdd(name);
            out.append(type.spacing + "public static class " + name.value + " extends Pointer {");
        }
        boolean done = false;
        while (!done && nextToken().type != -1) {
            switch (token.type) {
                case '(': function();     break;
                case ';': variable(name); break;
                case '#': macro();        break;
                case '{': block();        break;
                case '}': done = true;    break;
                default: pastTokens.add(token); break;
            }
        }
        for (Token t : pastTokens) {
            if (t.type == Token.COMMENT) {
                out.append(t.spacing + t.value);
            }
        }
        pastTokens.clear();
        if (block) {
            match('}');
            out.append(token.spacing + '}');
            if (nextToken().type == Token.IDENTIFIER) {
                name.value = token.value;
                nextToken();
            }
            match(';');
        }
        out.append(token.spacing);
    }

    void enumeration() throws IOException {
        match('{');

        Token type = commentBefore();
        Token name = futureTokens.getOrAdd(pastTokens.getLast());
        pastTokens.clear();
        String spacing = type.spacing.substring(type.spacing.lastIndexOf('\n') + 1);
        out.append(type.spacing + "/** enum " + name.value + " */" + lineSeparator);
        out.append(spacing + "public static final int");

        int count = 0;
        boolean done = false;
        while (!done && nextToken().type != -1) {
            switch (token.type) {
                case '}': done = true; // no break
                case ',':
                    if ((pastTokens.size() != 1 && pastTokens.size() != 3) ||
                            (pastTokens.size() == 3 && pastTokens.get(1).type != '=')) {
                        throw new RuntimeException(lineNumber + ": Could not parse enumerator");
                    }
                    out.append(pastTokens.get(0).spacing + pastTokens.get(0).value);
                    if (pastTokens.size() == 3) {
                        count = Integer.parseInt(pastTokens.get(2).value);
                    }
                    pastTokens.clear();
                    out.append(" = " + count++ + (done ? ";" : ","));
                    break;
                case Token.IDENTIFIER:
                case Token.NUMBER:
                default: pastTokens.add(token); break;
            }
        }
        if (nextToken().type == Token.IDENTIFIER) {
            name.value = token.value;
            nextToken();
        }
        match(';');
    }

    public void block() throws IOException {
        for (int i = 0; i < pastTokens.size(); i++) {
            String s = pastTokens.get(i).value;
            if (s.equals("struct") || s.equals("class") || s.equals("union")) {
                group();
                break;
            } else if (s.equals("enum")) {
                enumeration();
                break;
            }
        }
    }

    public void parse(String inputFilename, String outputFilename) throws IOException {
        input = new BufferedReader(new FileReader(inputFilename));
        out = new Writer() {
            @Override public void close() { }
            @Override public void flush() { }
            @Override public void write(char[] cbuf, int off, int len) { }
        };
        pastTokens.clear();
        futureTokens.clear();
        lineNumber = 0;
        group();
        input.close();

        input = new BufferedReader(new FileReader(inputFilename));
        out = new PrintWriter(System.out/*new FileWriter(outputFilename)*/);
        pastTokens.clear();
        futureTokens.reset();
        lineNumber = 0;
        group();
        input.close();
        out.close();
    }

    public static void main(String[] args) throws IOException {
        Reader r = new BufferedReader(new FileReader("/home/saudet/workspace/foo.h"/*args[0]*/));
        Writer w = new PrintWriter(System.out/*new FileWriter(args[1])*/);
        Parser parser = new Parser(r, w);
        parser.parse("/home/saudet/workspace/foo.h" /*args[0]*/, "moo");
        w.close();
    }
}
