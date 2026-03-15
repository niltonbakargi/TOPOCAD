package com.topocad.viewmodels

import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.topocad.models.Ponto
import com.topocad.utils.CsvProcessor
import java.io.File
import java.util.Locale

class MainViewModel : ViewModel() {

    private val _pontosList = MutableLiveData<List<Ponto>>()
    val pontosList: LiveData<List<Ponto>> get() = _pontosList

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    private var nomeArquivo: String = ""
    private var pontosFluxo: MutableList<Ponto> = mutableListOf()

    fun processarCsv(arquivo: File) {
        val pontos = CsvProcessor.processar(arquivo)
        if (pontos.isEmpty()) {
            _errorMessage.value = "Nenhum ponto encontrado no arquivo."
            return
        }
        nomeArquivo = arquivo.nameWithoutExtension
        _pontosList.value = pontos.sortedBy { it.cotaAltura }
    }

    fun reiniciarFluxoCota() {
        pontosFluxo = (_pontosList.value ?: emptyList())
            .sortedBy { it.cotaAltura }
            .toMutableList()
    }

    fun getCotaMaisBaixaAtual(): Double? = pontosFluxo.firstOrNull()?.cotaAltura

    fun removerCotaMaisBaixa(): Boolean {
        if (pontosFluxo.isNotEmpty()) pontosFluxo.removeAt(0)
        return pontosFluxo.isNotEmpty()
    }

    fun gerarESalvarScript(alturaDesejada: Double): File? {
        if (pontosFluxo.isEmpty()) return null

        val cotaBase = pontosFluxo.first().cotaAltura
        val ajuste = alturaDesejada - cotaBase

        val pontosAjustados = pontosFluxo
            .map { p -> p.copy(cotaAltura = p.cotaAltura + ajuste) }
            .sortedBy { it.descricao }

        val script = buildScript(pontosAjustados)

        return try {
            val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val arquivo = File(downloads, "${nomeArquivo}_script.scr")
            arquivo.writeText(script)
            arquivo
        } catch (e: Exception) {
            _errorMessage.value = "Erro ao salvar script: ${e.message}"
            null
        }
    }

    private fun buildScript(pontos: List<Ponto>): String {
        val sb = StringBuilder()

        // Layer 01: todos os pontos com nomes originais
        sb.append("-layer\nnew\n01\nset\n01\n\n")
        sb.append("insert pontos\n")
        var primeira = true
        for (ponto in pontos) {
            val coord = "${String.format(Locale.US, "%.3f", ponto.y)},${String.format(Locale.US, "%.3f", ponto.x)},${String.format(Locale.US, "%.3f", ponto.cotaAltura)}"
            val cota = String.format(Locale.US, "%.3f", ponto.cotaAltura)
            val linha = "$coord    $cota - ${ponto.descricao}\n"
            if (primeira) { sb.append(linha); primeira = false } else sb.append("  $linha")
        }

        // Layers seguintes: agrupados pelas 3 primeiras letras
        val porGrupo = pontos.groupBy { it.descricao.take(3).uppercase() }
        var layerNum = 2
        for ((grupo, grupoPontos) in porGrupo) {
            val layerName = layerNum.toString().padStart(2, '0')
            sb.append("-layer\nnew\n$layerName\nset\n$layerName\n\n")
            sb.append("insert pontos\n")
            var primeiraGrupo = true
            for (ponto in grupoPontos) {
                val coord = "${String.format(Locale.US, "%.3f", ponto.y)},${String.format(Locale.US, "%.3f", ponto.x)},${String.format(Locale.US, "%.3f", ponto.cotaAltura)}"
                val cota = String.format(Locale.US, "%.3f", ponto.cotaAltura)
                val linha = "$coord    $cota - $grupo\n"
                if (primeiraGrupo) { sb.append(linha); primeiraGrupo = false } else sb.append("  $linha")
            }
            layerNum++
        }
        return sb.toString()
    }
}
