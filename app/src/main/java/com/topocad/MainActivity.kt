package com.topocad

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.topocad.adapters.PontoAdapter
import com.topocad.databinding.ActivityMainBinding
import com.topocad.viewmodels.MainViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var adapter: PontoAdapter

    companion object {
        private const val REQUEST_LEGACY_PERMISSION = 100
        private const val REQUEST_MANAGE_STORAGE = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[MainViewModel::class.java]
        adapter = PontoAdapter()

        binding.recyclerViewPontos.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewPontos.adapter = adapter

        viewModel.pontosList.observe(this) { pontos ->
            adapter.updateData(pontos)
        }

        viewModel.errorMessage.observe(this) { msg ->
            if (!msg.isNullOrEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }

        binding.btnSelecionarCsv.setOnClickListener {
            verificarPermissaoEListarArquivos()
        }

        binding.btnGerarScript.setOnClickListener {
            if (viewModel.pontosList.value.isNullOrEmpty()) {
                Toast.makeText(this, "Nenhum ponto carregado.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            iniciarFluxoCota()
        }

        verificarPermissaoEListarArquivos()
    }

    private fun verificarPermissaoEListarArquivos() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                if (!Environment.isExternalStorageManager()) {
                    AlertDialog.Builder(this)
                        .setTitle("Permissão necessária")
                        .setMessage("Para listar arquivos CSV em Downloads, conceda acesso a todos os arquivos nas configurações.")
                        .setPositiveButton("Abrir configurações") { _, _ ->
                            val intent = Intent(
                                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                            @Suppress("DEPRECATION")
                            startActivityForResult(intent, REQUEST_MANAGE_STORAGE)
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                } else {
                    listarArquivosCsv()
                }
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        REQUEST_LEGACY_PERMISSION
                    )
                } else {
                    listarArquivosCsv()
                }
            }
            else -> listarArquivosCsv()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LEGACY_PERMISSION &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            listarArquivosCsv()
        } else if (requestCode == REQUEST_LEGACY_PERMISSION) {
            Toast.makeText(this, "Permissão negada.", Toast.LENGTH_LONG).show()
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MANAGE_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
                listarArquivosCsv()
            } else {
                Toast.makeText(this, "Permissão não concedida.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun listarArquivosCsv() {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val arquivos = downloads.listFiles { f -> f.extension.equals("csv", ignoreCase = true) }

        if (arquivos.isNullOrEmpty()) {
            Toast.makeText(this, "Nenhum arquivo CSV encontrado em Downloads.", Toast.LENGTH_LONG).show()
            return
        }

        val nomes = arquivos.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Selecionar arquivo CSV")
            .setItems(nomes) { _, index ->
                viewModel.processarCsv(arquivos[index])
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun iniciarFluxoCota() {
        viewModel.reiniciarFluxoCota()
        mostrarDialogoCota()
    }

    private fun mostrarDialogoCota() {
        val cotaMaisBaixa = viewModel.getCotaMaisBaixaAtual()
        if (cotaMaisBaixa == null) {
            Toast.makeText(this, "Não há mais pontos disponíveis.", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Cota de referência")
            .setMessage("Cota mais baixa atual: ${"%.3f".format(cotaMaisBaixa)}\n\nUsar como referência?")
            .setPositiveButton("Sim") { _, _ -> mostrarDialogoAlturaDesejada() }
            .setNegativeButton("Não") { _, _ ->
                val temMais = viewModel.removerCotaMaisBaixa()
                if (temMais) mostrarDialogoCota()
                else Toast.makeText(this, "Não há mais pontos disponíveis.", Toast.LENGTH_SHORT).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun mostrarDialogoAlturaDesejada() {
        val input = EditText(this).apply {
            hint = "Ex: 100.000"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                    android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
        }

        AlertDialog.Builder(this)
            .setTitle("Altura desejada")
            .setMessage("Digite a altura para a cota de referência:")
            .setView(input)
            .setPositiveButton("Gerar Script") { _, _ ->
                val altura = input.text.toString().replace(',', '.').toDoubleOrNull()
                if (altura == null) {
                    Toast.makeText(this, "Valor inválido.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val arquivo = viewModel.gerarESalvarScript(altura)
                if (arquivo != null) {
                    Toast.makeText(this, "Script salvo: ${arquivo.name}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}
