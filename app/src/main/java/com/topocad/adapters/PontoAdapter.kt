package com.topocad.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.topocad.models.Ponto

class PontoAdapter(private var pontos: List<Ponto> = emptyList()) :
    RecyclerView.Adapter<PontoAdapter.PontoViewHolder>() {

    class PontoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvId: TextView = itemView.findViewById(android.R.id.text1)
        val tvCoordenadas: TextView = itemView.findViewById(android.R.id.text2)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PontoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return PontoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PontoViewHolder, position: Int) {
        val ponto = pontos[position]
        holder.tvId.text = "ID: ${ponto.id} - ${ponto.descricao}"
        holder.tvCoordenadas.text = "X: ${ponto.x}  Y: ${ponto.y}  Cota: ${"%.3f".format(ponto.cotaAltura)}"
    }

    override fun getItemCount() = pontos.size

    fun updateData(novosPontos: List<Ponto>) {
        pontos = novosPontos
        notifyDataSetChanged()
    }
}
