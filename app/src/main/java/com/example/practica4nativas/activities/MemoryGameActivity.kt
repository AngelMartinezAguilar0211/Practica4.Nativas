package com.example.practica4nativas.activities

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import android.text.InputType
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.practica4nativas.databinding.ActivityMemoryGameBinding
import com.example.practica4nativas.utils.ThemeManager
import com.example.practica4nativas.data.MemoryGameState
import com.google.gson.Gson
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File

class MemoryGameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemoryGameBinding
    private var bloqueoToques = false

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityMemoryGameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Juego de Memoria"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        //Boton de siguiente nivel
        binding.btnSiguienteNivel.setOnClickListener {
            val siguienteNivel = juego.nivel + 1
            crearTablero(siguienteNivel)
            binding.btnSiguienteNivel.isEnabled = false
            binding.btnSiguienteNivel.visibility = View.GONE
        }
        //Botones guardar/cargar
        binding.btnGuardarPartida.setOnClickListener {
            mostrarDialogoGuardarPartida()
        }

        binding.btnVolverAlMenu.setOnClickListener {
            val intent = Intent(this, MemoryGameMenuActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }


        // AQUI iremos agregando la lógica del tablero y estado de juego
        //Opcion cargar o nueva partida

        val modo = intent.getStringExtra("modo")
        val archivo = intent.getStringExtra("archivo")
        val tipo = intent.getStringExtra("tipo")

        if (modo == "archivo" && archivo != null && tipo != null) {
            cargarDesdeArchivo(archivo, tipo)
        } else if (modo == "cargar") {
            cargarPartidaJson()
        } else {
            crearTablero(1)
        }




    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun generarCartasPorNivel(nivel: Int): List<String> {
        val emojiSet = listOf("🐶", "🐱", "🦊", "🐻", "🐸", "🐵", "🐼", "🐷", "🐯", "🐮", "🐔", "🐙")
        val pares = 2 + nivel // aumenta dificultad
        val emojisUsados = emojiSet.shuffled().take(pares)
        return (emojisUsados + emojisUsados).shuffled()
    }

    private fun crearTablero(nivel: Int) {
        val cartas = generarCartasPorNivel(nivel)
        val total = cartas.size
        val columnas = 4
        val filas = (total + columnas - 1) / columnas

        binding.gridLayout.removeAllViews()
        binding.gridLayout.columnCount = columnas
        binding.gridLayout.rowCount = filas

        cartas.forEachIndexed { index, emoji ->
            val textView = TextView(this).apply {
                text = "❓"
                textSize = 32f
                gravity = Gravity.CENTER
                setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
                setPadding(8, 8, 8, 8)
                setOnClickListener { manejarSeleccion(this, index) }
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }

            binding.gridLayout.addView(textView, params)
        }

        // Estado inicial
        juego = MemoryGameState(
            nivel = nivel,
            puntuacion = 0,
            movimientos = 0,
            tiempo = "00:00",
            tablero = cartas,
            descubiertos = MutableList(cartas.size) { false }
        )
        segundosTranscurridos = 0
        iniciarTemporizador()

        actualizarUI()
    }

    private var primeraCarta: Int? = null
    private lateinit var juego: MemoryGameState

    private fun manejarSeleccion(vista: TextView, indice: Int) {
        if (juego.descubiertos[indice] || bloqueoToques || indice == primeraCarta) return

        vista.text = juego.tablero[indice]

        if (primeraCarta == null) {
            primeraCarta = indice
        } else {
            val segundaCarta = indice
            val primera = primeraCarta!!
            bloqueoToques = true

            val iguales = juego.tablero[primera] == juego.tablero[segundaCarta]
            if (iguales) {
                juego.puntuacion += 10
                juego.descubiertos[primera] = true
                juego.descubiertos[segundaCarta] = true
                bloqueoToques = false
                primeraCarta = null
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    val primeraView = binding.gridLayout.getChildAt(primera) as? TextView
                    val segundaView = binding.gridLayout.getChildAt(segundaCarta) as? TextView
                    primeraView?.text = "❓"
                    segundaView?.text = "❓"
                    bloqueoToques = false
                    primeraCarta = null
                }, 600)
            }

            juego.movimientos++
            actualizarUI()

            if (juego.descubiertos.all { it }) {
                timer?.cancel()
                binding.btnSiguienteNivel.isEnabled = true
                binding.btnSiguienteNivel.visibility = View.VISIBLE
                Toast.makeText(this, "¡Nivel completado!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun actualizarUI() {
        binding.levelText.text = "Nivel: ${juego.nivel}"
        binding.scoreText.text = "Puntuación: ${juego.puntuacion}"
        binding.movesText.text = "Movimientos: ${juego.movimientos}"
        binding.timerText.text = "Tiempo: ${juego.tiempo}"
    }

    private var segundosTranscurridos = 0
    private var timer: CountDownTimer? = null

    private fun iniciarTemporizador() {
        timer?.cancel() // Reinicia si ya hay uno

        timer = object : CountDownTimer(Long.MAX_VALUE, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                segundosTranscurridos++
                val minutos = segundosTranscurridos / 60
                val segundos = segundosTranscurridos % 60
                juego = juego.copy(tiempo = String.format("%02d:%02d", minutos, segundos))
                binding.timerText.text = "Tiempo: ${juego.tiempo}"
            }

            override fun onFinish() {}
        }

        timer?.start()
    }

    private fun guardarPartidaJson(nombreArchivo: String) {
        val gson = Gson()
        val json = gson.toJson(juego)

        try {
            val file = File(filesDir, nombreArchivo)
            file.writeText(json)
            Toast.makeText(this, "Partida guardada en JSON", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar partida", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarPartidaXml(nombreArchivo: String) {
        try {
            val archivo = File(filesDir, nombreArchivo)
            val xml = buildString {
                append("<partida>\n")
                append("  <nivel>${juego.nivel}</nivel>\n")
                append("  <puntuacion>${juego.puntuacion}</puntuacion>\n")
                append("  <movimientos>${juego.movimientos}</movimientos>\n")
                append("  <tiempo>${juego.tiempo}</tiempo>\n")
                juego.tablero.forEach {
                    append("  <carta emoji=\"$it\" />\n")
                }
                juego.descubiertos.forEach {
                    append("  <descubierta valor=\"$it\" />\n")
                }
                append("</partida>")
            }
            archivo.writeText(xml)
            Toast.makeText(this, "Partida guardada en XML", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar XML", Toast.LENGTH_SHORT).show()
        }
    }

    private fun guardarPartidaTxt(nombreArchivo: String) {
        try {
            val archivo = File(filesDir, nombreArchivo)
            val contenido = buildString {
                appendLine(juego.nivel)
                appendLine(juego.puntuacion)
                appendLine(juego.movimientos)
                appendLine(juego.tiempo)
                appendLine(juego.tablero.joinToString(","))
                appendLine(juego.descubiertos.joinToString(","))
            }
            archivo.writeText(contenido)
            Toast.makeText(this, "Partida guardada en TXT", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar TXT", Toast.LENGTH_SHORT).show()
        }
    }



    private fun cargarPartidaJson() {
        try {
            val file = File(filesDir, "partida_guardada.json")
            if (!file.exists()) {
                Toast.makeText(this, "No hay partida guardada", Toast.LENGTH_SHORT).show()
                return
            }

            val json = file.readText()
            val gson = Gson()
            juego = gson.fromJson(json, MemoryGameState::class.java)

            reconstruirTableroDesdeEstado()
            Toast.makeText(this, "Partida cargada desde JSON", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al cargar partida", Toast.LENGTH_SHORT).show()
        }
    }

    private fun reconstruirTableroDesdeEstado() {
        val cartas = juego.tablero
        val total = cartas.size
        val columnas = 4
        val filas = (total + columnas - 1) / columnas

        binding.gridLayout.removeAllViews()
        binding.gridLayout.columnCount = columnas
        binding.gridLayout.rowCount = filas

        cartas.forEachIndexed { index, emoji ->
            val textView = TextView(this).apply {
                text = if (juego.descubiertos[index]) emoji else "❓"
                textSize = 32f
                gravity = Gravity.CENTER
                setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
                setPadding(8, 8, 8, 8)
                setOnClickListener { manejarSeleccion(this, index) }
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }

            binding.gridLayout.addView(textView, params)
        }

        segundosTranscurridos = juego.tiempo.split(":").let { (m, s) -> m.toInt() * 60 + s.toInt() }
        iniciarTemporizador()
        actualizarUI()
    }

    private fun cargarDesdeArchivo(nombreArchivo: String, tipo: String) {
        val file = File(filesDir, nombreArchivo)

        try {
            val contenido = file.readText()

            juego = when (tipo) {
                "json" -> {
                    val gson = Gson()
                    gson.fromJson(contenido, MemoryGameState::class.java)
                }

                "xml" -> {
                    val factory = XmlPullParserFactory.newInstance()
                    val parser = factory.newPullParser()
                    parser.setInput(contenido.reader())

                    var nivel = 1
                    var puntuacion = 0
                    var movimientos = 0
                    var tiempo = "00:00"
                    val tablero = mutableListOf<String>()
                    val descubiertos = mutableListOf<Boolean>()

                    var event = parser.eventType
                    while (event != XmlPullParser.END_DOCUMENT) {
                        if (event == XmlPullParser.START_TAG) {
                            when (parser.name) {
                                "nivel" -> nivel = parser.nextText().toInt()
                                "puntuacion" -> puntuacion = parser.nextText().toInt()
                                "movimientos" -> movimientos = parser.nextText().toInt()
                                "tiempo" -> tiempo = parser.nextText()
                                "carta" -> tablero.add(parser.getAttributeValue(null, "emoji"))
                                "descubierta" -> descubiertos.add(parser.getAttributeValue(null, "valor").toBoolean())
                            }
                        }
                        event = parser.next()
                    }

                    MemoryGameState(nivel, puntuacion, tablero, descubiertos, movimientos, tiempo)
                }

                "txt" -> {
                    val lineas = contenido.lines()
                    val nivel = lineas[0].toInt()
                    val puntuacion = lineas[1].toInt()
                    val movimientos = lineas[2].toInt()
                    val tiempo = lineas[3]
                    val tablero = lineas[4].split(",")
                    val descubiertos = lineas[5].split(",").map { it.toBoolean() }.toMutableList()

                    MemoryGameState(nivel, puntuacion, tablero, descubiertos, movimientos, tiempo)
                }

                else -> throw IllegalArgumentException("Tipo de archivo no soportado")
            }

            reconstruirTableroDesdeEstado()
            Toast.makeText(this, "Cargado: $nombreArchivo", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al cargar $nombreArchivo", Toast.LENGTH_SHORT).show()
            crearTablero(1)
        }
    }
    private fun mostrarDialogoGuardarPartida() {
        val formatos = arrayOf("JSON", "XML", "TXT")
        val input = EditText(this).apply {
            hint = "Nombre del archivo (sin extensión)"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        AlertDialog.Builder(this)
            .setTitle("Guardar partida")
            .setView(input)
            .setSingleChoiceItems(formatos, 0, null)
            .setPositiveButton("Guardar") { dialog, _ ->
                val dialogView = (dialog as AlertDialog)
                val selected = dialogView.listView.checkedItemPosition
                val nombre = input.text.toString().trim()

                if (nombre.isBlank()) {
                    Toast.makeText(this, "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val extension = formatos[selected].lowercase()
                val nombreArchivo = "$nombre.$extension"

                when (extension) {
                    "json" -> guardarPartidaJson(nombreArchivo)
                    "xml" -> guardarPartidaXml(nombreArchivo)
                    "txt" -> guardarPartidaTxt(nombreArchivo)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }





}
