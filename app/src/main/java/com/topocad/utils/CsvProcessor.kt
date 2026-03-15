package com.topocad.utils

import com.topocad.models.Ponto
import java.io.File

object CsvProcessor {

    private val DELIMITERS = listOf(',', ';', '\t')

    fun processar(arquivo: File): List<Ponto> {
        val linhas = arquivo.readLines()
        if (linhas.size < 2) return emptyList()

        val delimitador = detectarDelimitador(linhas[0]) ?: return emptyList()
        val pontos = mutableListOf<Ponto>()

        for (i in 1 until linhas.size) {
            val linha = linhas[i].trim()
            if (linha.isEmpty()) continue
            try {
                val partes = linha.split(delimitador)
                if (partes.size >= 5) {
                    pontos.add(
                        Ponto(
                            id = partes[0].trim(),
                            x = partes[1].trim().replace(',', '.').toDouble(),
                            y = partes[2].trim().replace(',', '.').toDouble(),
                            cotaAltura = partes[3].trim().replace(',', '.').toDouble(),
                            descricao = partes[4].trim()
                        )
                    )
                }
            } catch (e: Exception) {
                // linha inválida, ignorar
            }
        }
        return pontos
    }

    private fun detectarDelimitador(header: String): Char? {
        return DELIMITERS.maxByOrNull { delim -> header.count { c -> c == delim } }
            ?.takeIf { delim -> header.count { c -> c == delim } > 0 }
    }
}
