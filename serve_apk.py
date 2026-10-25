import http.server
import socketserver
import os
import qrcode
import io

PORT = 8080
DIRECTORY = "/home/jorge/financialApp/app/build/outputs/apk/debug"

class CustomHandler(http.server.SimpleHTTPRequestHandler):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, directory=DIRECTORY, **kwargs)

def print_qr(url):
    qr = qrcode.QRCode()
    qr.add_data(url)
    qr.make()
    f = io.StringIO()
    qr.print_ascii(out=f)
    f.seek(0)
    print(f.read())

if __name__ == "__main__":
    os.chdir(DIRECTORY)
    # Copiar a FinanzasPro.apk si no existe
    if not os.path.exists("FinanzasPro.apk"):
        os.system("cp app-debug.apk FinanzasPro.apk")
        
    url_wifi = f"http://192.168.1.1:{PORT}/FinanzasPro.apk"
    url_wsl = f"http://172.30.81.183:{PORT}/FinanzasPro.apk"
    
    print("\n" + "="*60)
    print("🚀 SERVIDOR DE DESCARGA DIRECTA DE APK ACTIVO")
    print("="*60)
    print(f"Enlace de descarga en tu red Wi-Fi: {url_wifi}")
    print(f"Enlace alternativo: {url_wsl}")
    print("\n📲 Escanea este código QR con la cámara de tu Xiaomi 14T Pro:")
    print_qr(url_wifi)
    print("="*60)
    print("Presiona Ctrl+C cuando termines de descargar el archivo.\n")
    
    # También guardar imagen QR en el escritorio de Windows
    try:
        img = qrcode.make(url_wifi)
        img.save("/mnt/c/Users/USER/Desktop/Escanear_Para_Instalar_FinanzasPro.png")
        print("💾 Imagen QR guardada en tu Escritorio: 'Escanear_Para_Instalar_FinanzasPro.png'")
    except Exception as e:
        pass

    with socketserver.TCPServer(("0.0.0.0", PORT), CustomHandler) as httpd:
        httpd.serve_forever()
