#!/usr/bin/env python3
import subprocess
import sys
import os
import signal
import threading

PROJECT_ROOT = os.path.dirname(os.path.abspath(__file__))
BACKEND_DIR = os.path.join(PROJECT_ROOT, "backend")
FRONTEND_DIR = os.path.join(PROJECT_ROOT, "frontend")

processes = []

def stream_logs(process, prefix):
    """Streams stdout/stderr of a subprocess with a labeled prefix."""
    try:
        for line in iter(process.stdout.readline, ''):
            if not line:
                break
            print(f"{prefix} {line.rstrip()}", flush=True)
    except Exception as e:
        print(f"{prefix} Log streaming error: {e}", flush=True)

def cleanup(signum=None, frame=None):
    """Gracefully terminates all child processes."""
    print("\n[HERON] Shutting down services...", flush=True)
    for proc in processes:
        if proc.poll() is None:
            try:
                proc.terminate()
                proc.wait(timeout=3)
            except subprocess.TimeoutExpired:
                proc.kill()
    print("[HERON] All services stopped.", flush=True)
    sys.exit(0)

def main():
    signal.signal(signal.SIGINT, cleanup)
    signal.signal(signal.SIGTERM, cleanup)

    print("=" * 60)
    print(" 🚀 HERON TRADING PLATFORM - UNIFIED RUNNER ")
    print("=" * 60)
    print(f"Backend path:  {BACKEND_DIR}")
    print(f"Frontend path: {FRONTEND_DIR}")
    print("-" * 60)

    # 1. Start Backend Process
    print("[HERON] Launching Spring Boot Backend...", flush=True)
    mvn_cmd = "./mvnw" if os.name != "nt" else "mvnw.cmd"
    backend_proc = subprocess.Popen(
        [os.path.join(BACKEND_DIR, mvn_cmd), "spring-boot:run"],
        cwd=BACKEND_DIR,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1
    )
    processes.append(backend_proc)

    # Stream backend logs in a separate thread
    backend_thread = threading.Thread(
        target=stream_logs,
        args=(backend_proc, "[\033[36mBACKEND\033[0m]"),
        daemon=True
    )
    backend_thread.start()

    # 2. Start Frontend Process
    print("[HERON] Launching Vite + React Frontend...", flush=True)
    npm_cmd = "npm" if os.name != "nt" else "npm.cmd"
    frontend_proc = subprocess.Popen(
        [npm_cmd, "run", "dev"],
        cwd=FRONTEND_DIR,
        stdout=subprocess.PIPE,
        stderr=subprocess.STDOUT,
        text=True,
        bufsize=1
    )
    processes.append(frontend_proc)

    # Stream frontend logs in a separate thread
    frontend_thread = threading.Thread(
        target=stream_logs,
        args=(frontend_proc, "[\033[32mFRONTEND\033[0m]"),
        daemon=True
    )
    frontend_thread.start()

    print("\n[HERON] Services are starting up! Press Ctrl+C to stop both.\n", flush=True)

    # Keep main thread alive waiting for subprocesses or interrupt
    try:
        while True:
            # Check if any process died unexpectedly
            for proc in processes:
                if proc.poll() is not None:
                    print(f"\n[HERON] A service exited with code {proc.returncode}.", flush=True)
                    cleanup()
            threading.Event().wait(1)
    except KeyboardInterrupt:
        cleanup()

if __name__ == "__main__":
    main()
