package it.algos.smsgateway

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers

class LogCatViewModel : ViewModel() {
    fun logCatOutput() = liveData(viewModelScope.coroutineContext + Dispatchers.IO) {
        Runtime.getRuntime().exec("logcat -c")
        Runtime.getRuntime().exec("logcat")
                .inputStream
                .bufferedReader()
                .useLines { lines -> lines.forEach { line -> emit(line) }
                }
    }
}
