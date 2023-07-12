//
// ========================================================================
// Copyright (c) 1995 Mort Bay Consulting Pty Ltd and others.
//
// This program and the accompanying materials are made available under the
// terms of the Eclipse Public License v. 2.0 which is available at
// https://www.eclipse.org/legal/epl-2.0, or the Apache License, Version 2.0
// which is available at https://www.apache.org/licenses/LICENSE-2.0.
//
// SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
// ========================================================================
//

package org.eclipse.jetty.start;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandLineBuilder
{
    public static File findExecutable(File root, String path)
    {
        String npath = path.replace('/', File.separatorChar);
        File exe = new File(root, npath);
        if (!exe.exists())
        {
            return null;
        }
        return exe;
    }

    public static String findJavaBin()
    {
        File javaHome = new File(System.getProperty("java.home"));
        if (!javaHome.exists())
        {
            return null;
        }

        File javabin = findExecutable(javaHome, "bin/java");
        if (javabin != null)
        {
            return javabin.getAbsolutePath();
        }

        javabin = findExecutable(javaHome, "bin/java.exe");
        if (javabin != null)
        {
            return javabin.getAbsolutePath();
        }

        return "java";
    }

    private final StringBuilder commandLine = new StringBuilder();
    private final List<String> args = new ArrayList<>();
    private final String separator;

    public CommandLineBuilder()
    {
        this(false);
    }

    public CommandLineBuilder(boolean multiline)
    {
        separator = multiline ? (" \\" + System.lineSeparator() + "  ") : " ";
    }

    /**
     * Quote a string suitable for use with a command line shell using double quotes.
     * <p>This method applies doubles quotes suitable for a POSIX compliant shell:
     * Enclosing characters within double quotes preserves the literal meaning of all characters except
     * dollarsign ($), backquote (`), and backslash (\).
     * The backslash inside double quotes is historically weird, and serves
     * to quote only the following characters: {@code $ ` " \ newline}.
     * Otherwise, it remains literal.
     *
     * @param input The string to quote if needed
     * @return The quoted string or the original string if quotes are not necessary
     */
    public static String shellQuoteIfNeeded(String input)
    {
        if (input == null)
            return null;
        if (input.length() == 0)
            return "\"\"";

        int i = 0;
        boolean needsQuoting = false;
        while (!needsQuoting && i < input.length())
        {
            char c = input.charAt(i++);

            // needs quoting unless a limited set of known good characters
            needsQuoting = !(
                (c >= 'A' && c <= 'Z') ||
                (c >= 'a' && c <= 'z') ||
                (c >= '0' && c <= '9') ||
                c == '/' ||
                c == ':' ||
                c == '.' ||
                c == ',' ||
                c == '-' ||
                c == '_'
                );
        }

        if (!needsQuoting)
            return input;

        StringBuilder builder = new StringBuilder(input.length() * 2);
        builder.append('"');
        builder.append(input, 0, --i);

        while (i < input.length())
        {
            char c = input.charAt(i++);
            switch (c)
            {
                case '"':
                case '\\':
                case '`':
                case '$':
                case '\n':
                    builder.append('\\').append(c);
                    break;
                default: builder.append(c);
            }
        }

        builder.append('"');

        return builder.toString();
    }

    /**
     * Add a simple argument to the command line, quoted if necessary.
     *
     * @param arg the simple argument to add
     */
    public void addArg(String arg)
    {
        if (arg != null)
        {
            if (commandLine.length() > 0)
                commandLine.append(separator);
            args.add(arg);
            commandLine.append(shellQuoteIfNeeded(arg));
        }
    }

    /**
     * Add a "name=value" style argument to the command line with
     * name and value quoted if necessary.
     * @param name the name
     * @param value the value
     */
    public void addArg(String name, String value)
    {
        Objects.requireNonNull(name);

        if (commandLine.length() > 0)
            commandLine.append(separator);

        if ((value != null) && (value.length() > 0))
        {
            args.add(name + "=" + value);
            commandLine.append(shellQuoteIfNeeded(name)).append('=').append(shellQuoteIfNeeded(value));
        }
        else
        {
            args.add(name);
            commandLine.append(shellQuoteIfNeeded(name));
        }
    }

    /**
     * Add a "-Oname=value" style argument to the command line with
     * name and value quoted if necessary.
     * @param option the option
     * @param name the name
     * @param value the value
     */
    public void addArg(String option, String name, String value)
    {
        Objects.requireNonNull(option);

        if (commandLine.length() > 0)
            commandLine.append(separator);

        if (name == null || name.length() == 0)
        {
            commandLine.append(option);
            args.add(option);
        }
        else if ((value != null) && (value.length() > 0))
        {
            args.add(option + name + "=" + value);
            commandLine.append(option).append(shellQuoteIfNeeded(name)).append('=').append(shellQuoteIfNeeded(value));
        }
        else
        {
            args.add(option + name);
            commandLine.append(option).append(shellQuoteIfNeeded(name));
        }
    }

    public List<String> getArgs()
    {
        return args;
    }

    @Override
    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        for (String arg : args)
        {
            if (buf.length() > 0)
                buf.append(' ');
            buf.append(arg); // we assume escaping has occurred during addArg
        }

        return buf.toString();
    }

    /**
     * A version of {@link #toString()} where every arg is evaluated for potential {@code '} (single-quote tick) wrapping.
     *
     * @return the toString but each arg that has spaces is surrounded by {@code '} (single-quote tick)
     */
    public String toCommandLine()
    {
        return commandLine.toString();
    }

    public void debug()
    {
        if (!StartLog.isDebugEnabled())
        {
            return;
        }

        int len = args.size();
        StartLog.debug("Command Line: %,d entries", args.size());
        for (int i = 0; i < len; i++)
        {
            StartLog.debug(" [%d]: \"%s\"", i, args.get(i));
        }
    }
}
