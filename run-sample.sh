#!/usr/bin/env bash

mvn compile exec:java -Dexec.mainClass=com.vinyldns.sample.App -Dexec.cleanupDaemonThreads=false
