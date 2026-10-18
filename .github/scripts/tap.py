"""Taps the first UI node whose text or content description matches argv[1] (uiautomator)."""
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
    subprocess.run(["adb", "shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2)])


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
