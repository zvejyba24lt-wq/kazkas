"""Taps the first UI node whose text or content description matches one of the arguments (uiautomator).

With LONG_MS set, presses for that many milliseconds instead; DY shifts the press up (negative) or down in px.
"""
import os
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET


def dump():
    subprocess.run(["adb", "shell", "uiautomator", "dump", "/sdcard/ui.xml"], capture_output=True)
    return subprocess.run(["adb", "exec-out", "cat", "/sdcard/ui.xml"], capture_output=True, text=True).stdout


def tap_node(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    x, y = (x1 + x2) // 2, (y1 + y2) // 2 + int(os.environ.get("DY", "0"))
    long_ms = os.environ.get("LONG_MS")
    if long_ms:
        subprocess.run(["adb", "shell", "input", "swipe", str(x), str(y), str(x), str(y), long_ms])
    else:
        subprocess.run(["adb", "shell", "input", "tap", str(x), str(y)])


def dismiss_anr(root):
    """A slow CI emulator often shows "System UI isn't responding"; answer it with Wait."""
    if not any("isn't responding" in (n.get("text") or "") for n in root.iter("node")):
        return False
    for node in root.iter("node"):
        if node.get("text") == "Wait":
            tap_node(node)
            print("dismissed ANR dialog")
            time.sleep(2)
            return True
    return False


def main(targets):
    for _ in range(8):
        try:
            root = ET.fromstring(dump())
        except ET.ParseError:
            time.sleep(1.5)
            continue
        if dismiss_anr(root):
            continue
        if not targets:
            return 0
        for node in root.iter("node"):
            if node.get("text") in targets or node.get("content-desc") in targets:
                tap_node(node)
                print("tapped", node.get("text") or node.get("content-desc"))
                return 0
        time.sleep(1.5)
    print("not found:", targets)
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
