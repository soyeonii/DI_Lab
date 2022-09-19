package com.example.diapp

import android.content.Context

class Controller(private var context: Context) {
    fun run(): ArrayList<String> {
        val recognize = Recognize(context)
        return recognize.getResult(recognize.caseCheck())
    }

    //fun run(){
    //     val case: String
    //     val recognize = Recognize(context)
    //     recognize.preProcess()
    //     recognize.consonantVowelData()
    // }
}