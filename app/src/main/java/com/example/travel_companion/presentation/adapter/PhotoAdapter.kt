package com.example.travel_companion.presentation.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.travel_companion.data.local.entity.PhotoEntity
import com.example.travel_companion.databinding.ItemPhotoBinding

//Estende ListAdapter, che semplifica la gestione di liste dinamiche e ottimizza l'aggiornamento della UI tramite DiffUtil
class PhotoAdapter: ListAdapter<PhotoEntity, PhotoAdapter.PhotoViewHolder>(DiffCallback) {

    //Usa la libreria Glide per caricare l’immagine da un URI e mostrarla nell’ImageView, con centerCrop per un ritaglio uniforme.
    //Ogni Photo ha un URI salvato nel database, che viene convertito in oggetto Uri e caricato.
    inner class PhotoViewHolder(private val binding: ItemPhotoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(photo: PhotoEntity) {
            Glide.with(binding.root.context)
                .load(Uri.parse(photo.uri))
                .centerCrop()
                .into(binding.imageView)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    //DiffUtil.ItemCallback ottimizza il rendering confrontando id e contenuto degli oggetti Photo,
    // migliorando le performance rispetto al tradizionale notifyDataSetChanged().
    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<PhotoEntity>() {
            override fun areItemsTheSame(oldItem: PhotoEntity, newItem: PhotoEntity): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: PhotoEntity, newItem: PhotoEntity): Boolean = oldItem == newItem
        }
    }
}