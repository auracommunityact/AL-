package com.example.ui.pdf.viewmodels

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ImageToPdfViewModel : ViewModel() {
    private val _images = MutableStateFlow<List<Bitmap>>(emptyList())
    val images = _images.asStateFlow()

    private val _pdfName = MutableStateFlow("NewDocument.pdf")
    val pdfName = _pdfName.asStateFlow()

    fun addImages(newImages: List<Bitmap>) {
        _images.value = _images.value + newImages
    }

    fun removeImage(index: Int) {
        val list = _images.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _images.value = list
        }
    }

    fun reorderImages(from: Int, to: Int) {
        val list = _images.value.toMutableList()
        val item = list.removeAt(from)
        list.add(to, item)
        _images.value = list
    }

    fun setPdfName(name: String) {
        _pdfName.value = name
    }
}
