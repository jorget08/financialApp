# Finanzas Pro AI (Xiaomi HyperOS & Android Edition)

Aplicación móvil de finanzas personales ultrarrápida, con **fricción cero (Zero-Friction UX)**, widget de escritorio nativo interactivo con Jetpack Glance, tabla de interés compuesto no lineal y asesor financiero con Inteligencia Artificial.

---

## 🚀 Arquitectura y Tecnologías Clave

* **Lenguaje:** Kotlin 2.0+ con Jetpack Compose y Material 3.
* **Optimización de Pantalla:** Diseñada para pantallas CrystalRes AMOLED de 144Hz (Xiaomi 14T Pro).
* **Widget de Escritorio:** **Jetpack Glance (`androidx.glance`)** con ejecución en segundo plano (<30ms) para registro directo de gastos sin abrir la aplicación.
* **Persistencia Local:** **Room Database (SQLite)** con Flow reactivo y arquitectura Local-First.
* **Seguridad de API Key:** **Android Keystore con `EncryptedSharedPreferences`** (cifrado AES256-GCM por hardware).
* **Motor NLP Local:** Regex categorizador instantáneo (0ms, 0 tokens) con fallback a LLM (Gemini / OpenAI).

---

## 📁 Estructura del Proyecto

```
app/src/main/
├── AndroidManifest.xml
├── java/com/financialapp/
│   ├── FinancialApplication.kt              # Inicialización de base de datos y servicios
│   ├── domain/
│   │   ├── engine/
│   │   │   ├── CompoundInterestEngine.kt    # Cálculo TEA -> TEM y tabla mes a mes
│   │   │   └── NaturalLanguageExpenseParser.kt # Parser tipo WhatsApp ("Gasté 15 en café")
│   │   └── model/
│   │       └── MonthlyCompoundRecord.kt     # Modelos de desglose no lineal
│   ├── data/
│   │   ├── local/
│   │   │   ├── AppDatabase.kt               # Base de datos Room con datos semilla
│   │   │   ├── entities/                    # Transacciones, Categorías, Cuentas de Ahorro
│   │   │   └── dao/                         # TransactionDao, CategoryDao, SavingsDao
│   │   └── remote/ai/
│   │       ├── AISettingsManager.kt         # Almacenamiento cifrado en Keystore
│   │       └── AIAssistantService.kt        # Prompt y llamada a Gemini / OpenAI
│   ├── widget/
│   │   ├── QuickExpenseGlanceWidget.kt      # Widget interactivo de escritorio
│   │   ├── QuickExpenseGlanceReceiver.kt    # Receptor de actualización
│   │   └── QuickExpenseActionCallback.kt    # Inserción en BD en <30ms desde el widget
│   └── ui/
│       ├── MainActivity.kt                  # Host de navegación y barra inferior
│       ├── theme/                           # Paleta Dark Mode OLED optimizada
│       └── screens/
│           ├── home/HomeScreen.kt           # Dashboard de balance y botones rápidos
│           ├── quickentry/QuickExpenseDialogActivity.kt # Teclado flotante translúcido (<50ms)
│           ├── savings/SavingsCompoundScreen.kt # Tabla interactiva de interés compuesto
│           ├── chat/QuickChatScreen.kt       # Simulador WhatsApp y receptor de texto compartido
│           ├── advisor/AIAdvisorScreen.kt   # Diagnóstico y auditoría con IA
│           └── settings/SettingsScreen.kt   # Configuración segura de API Key
```

---

## 💡 Módulos Implementados

### 1. Widget de Pantalla de Inicio (Xiaomi HyperOS / Android)
- Ubicado en el launcher del teléfono.
- Botones rápidos directos (ej. `☕ $10`, `🍔 $25`, `🚕 $15`). Al tocarlos, un `ActionCallback` de Glance inserta la transacción en Room y refresca el contador en menos de **30 milisegundos**.
- Botón `⚡ + Gasto` que levanta la actividad translúcida `QuickExpenseDialogActivity` con teclado numérico integrado.

### 2. Cuenta de Ahorros e Interés Compuesto No Lineal
- Conversión exacta de Tasa Efectiva Anual a Tasa Efectiva Mensual:
  $$\text{TEM} = (1 + \text{TEA})^{1/12} - 1$$
- Tabla de control mes a mes que muestra:
  - Capital inicial y aporte mensual.
  - Interés generado en el mes (aumenta exponencialmente cada ciclo).
  - Saldo acumulado real vs. la curva ideal matemática.

### 3. Registro por WhatsApp / Lenguaje Natural
- Pantalla interactiva estilo chat.
- Procesa oraciones como:
  - `"Gasté 15 en café"` $\to$ Gasto $15.00 en Comida & Bebida.
  - `"Entraron 500 de sueldo"` $\to$ Ingreso $500.00 en Ingresos.
  - `"35 taxi"` $\to$ Gasto $35.00 en Transporte.
- Soporta la función **Compartir (Share Sheet)** de Android: selecciona un texto en WhatsApp $\to$ "Compartir con Finanzas Pro AI" $\to$ la app lo procesa automáticamente.

### 4. Asesor Financiero con IA Integrada
- Utiliza la propia clave de API del usuario (Google Gemini o OpenAI).
- Realiza una auditoría exhaustiva:
  - Detecta fugas de capital y gastos hormiga.
  - Calcula la tasa de ahorro real `(Ingresos - Gastos) / Ingresos` y emite alerta si es menor al 20%.
  - Compara la TEA del banco del usuario frente a tasas de cuentas de alto rendimiento del mercado.
  - Brinda 3 acciones de impacto inmediato y 1 meta patrimonial a 12 meses.
