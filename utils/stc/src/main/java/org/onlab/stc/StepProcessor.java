/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onlab.stc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import static java.lang.String.format;
import static org.onlab.stc.Coordinator.print;

/**
 * Manages execution of the specified step or a group.
 */
class StepProcessor implements Runnable {

    private static final int FAIL = -1;

    static String launcher = "stc-launcher ";

    private final Step step;
    private final boolean skip;
    private final File logDir;

    private Process process;
    private StepProcessListener delegate;

    /**
     * Creates a process monitor.
     *
     * @param step     step or group to be executed
     * @param skip     indicates the process should not actually execute
     * @param logDir   directory where step process log should be stored
     * @param delegate process lifecycle listener
     */
    StepProcessor(Step step, boolean skip, File logDir, StepProcessListener delegate) {
        this.step = step;
        this.skip = skip;
        this.logDir = logDir;
        this.delegate = delegate;
    }

    @Override
    public void run() {
        int code = FAIL;
        delegate.onStart(step);
        if (!skip) {
            code = execute();
        }
        delegate.onCompletion(step, code);
    }

    /**
     * Executes the step process.
     *
     * @return exit code
     */
    private int execute() {
        try (PrintWriter pw = new PrintWriter(logFile())) {
            process = Runtime.getRuntime().exec(command());
            processOutput(pw);

            // Wait for the process to complete and get its exit code.
            if (process.isAlive()) {
                process.waitFor();
            }
            return process.exitValue();

        } catch (IOException e) {
            print("Unable to run step %s using command %s", step.name(), step.command());
        } catch (InterruptedException e) {
            print("Step %s interrupted", step.name());
        }
        return FAIL;
    }

    /**
     * Returns ready-to-run command for the step.
     *
     * @return command to execute
     */
    private String command() {
        return format("%s %s %s %s", launcher,
                      step.env() != null ? step.env() : "-",
                      step.cwd() != null ? step.cwd() : "-",
                      step.command());
    }

    /**
     * Captures output of the step process.
     *
     * @param pw print writer to send output to
     * @throws IOException if unable to read output or write logs
     */
    private void processOutput(PrintWriter pw) throws IOException {
        InputStream out = process.getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(out));

        // Slurp its combined stderr/stdout
        String line;
        while ((line = br.readLine()) != null) {
            pw.println(line);
            delegate.onOutput(step, line);
        }
    }

    /**
     * Returns the log file for the step output.
     *
     * @return log file
     */
    private File logFile() {
        return new File(logDir, step.name() + ".log");
    }

}
