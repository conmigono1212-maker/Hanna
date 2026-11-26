"""Prueba de humo del APK en un emulador que ya tiene ADB disponible."""
import re
import subprocess
import time
import xml.etree.ElementTree as ET


def adb(*args):
    return subprocess.check_output(["adb", *args], text=True)


def screen():
    adb("shell", "uiautomator", "dump", "/sdcard/hannah-window.xml")
    return list(ET.fromstring(adb("shell", "cat", "/sdcard/hannah-window.xml")).iter("node"))


def find(label, timeout=45, *, field=False):
    end = time.monotonic() + timeout
    while time.monotonic() < end:
        for node in screen():
            if (field and node.get("class") == "android.widget.EditText") or (
                not field and label in node.get("text", "")
            ):
                return node
        time.sleep(1)
    print("Pantalla:", [n.get("text") for n in screen() if n.get("text")], flush=True)
    print(adb("logcat", "-b", "crash", "-d")[-6000:], flush=True)
    print(adb("logcat", "-d", "-t", "2000")[-4000:], flush=True)
    raise RuntimeError("No apareció " + label)


def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.get("bounds")))
    adb("shell", "input", "tap", str((x1+x2)//2), str((y1+y2)//2))


def send(words):
    tap(find("campo", field=True))
    adb("shell", "input", "text", words.replace(" ", "%s"))
    adb("shell", "input", "keyevent", "4")
    tap(find("Enviar"))


apk = "android/app/build/outputs/apk/debug/app-debug.apk"
adb("install", "-r", apk)
adb("shell", "am", "start", "-W", "-n", "com.cyberassistant/.ui.MainActivity")
tap(find("Plataformas"))
find("Navegador")
adb("shell", "input", "keyevent", "4")
find("HANNAH")
assert all(n.get("text") not in ("Conectar", "Ajustes") for n in screen())
print("Menú y navegador verificados.", flush=True)
send("busca en internet Bogota")
find("https://", 90)
print("La búsqueda devolvió enlaces.", flush=True)
# Volver al inicio con la conversación vacía para probar el chat sin red.
adb("shell", "am", "force-stop", "com.cyberassistant")
adb("shell", "am", "start", "-W", "-n", "com.cyberassistant/.ui.MainActivity")
tap(find("Descargar modelo"))
end = time.monotonic() + 600
while time.monotonic() < end:
    nodes = screen()
    if not any("Descargar modelo" in n.get("text", "") or "Descargando" in n.get("text", "") for n in nodes):
        break
    time.sleep(3)
else:
    raise RuntimeError("No terminó la descarga del modelo")
adb("shell", "cmd", "connectivity", "airplane-mode", "enable")
send("Di hola en castellano.")
find("Escuchar respuesta", 300)
nodes = screen()
print("Respuesta sin red:", [n.get("text") for n in nodes if n.get("text")], flush=True)
