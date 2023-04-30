package com.ong.readmoretextview

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.text.bold
import androidx.core.text.buildSpannedString
import androidx.core.text.color

/**
 * 동적으로 마지막 텍스트에 '더보기' 텍스트를 넣어주는 커스텀뷰
 * '더보기' text, '더보기' color xml로 원하는 값 주입
 * readMoreText.width <= layout.width 조건 내에서 항상 원본 텍스트의 마지막 행의 마지막 열에 필요한 만큼 글자 지우고 삽입
 * width : match_constraint or match_parent or ~dp  (no wrap_content)
 * height : match_constraint or wrap_content or match_parent or ~dp (all)
 * */
class ReadMoreTextView : AppCompatTextView {
    constructor(context: Context) : super(context, null)

    //textStyle 미적용
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs,0) {
        getStyleableAttrs(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        getStyleableAttrs(attrs)
    }

    /*logging*/
    private val TAG = this::class.java.simpleName
    private var measureCnt = 0
    private var sizeChangeCnt = 0
    private var layoutCnt = 0
    private var setTextCnt = 0
    private var drawCnt = 0

    /*attrs*/
    private var readMoreText: String = ""

    @ColorInt
    private var readMoreTextColor: Int = -1

    /*flags*/
    private var isNeedReadMoreText = false
    private var isFinishCutWidth = false

    /*private property*/
    private var collapseText: CharSequence = ""
    private lateinit var truncatedOriginText: StringBuffer
    private var originLineCount = 0

    /*
    * ===============================================================
    *                       override method
    * ===============================================================
    * */

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        printMeasure("${++measureCnt}")
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        printOnLayout("${++layoutCnt}")
    }

    /**
     * 행 제거 로직
     * layout.height : 내용의 크기
     * height : 뷰에 할당된 크기
     * layout.height > height -> 열 제거
     * 열 제거 로직 완료 후 다시 onSizeChanged 호출되는 경우는 pass
     * setText를 반복호출하여 UI Thread에서 반복이 일어나므로 최적화 필요 -> 본문 텍스트 - 사이즈 차이가 큰 경우 ANR 위험
     */

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        printOnSizeChanged("${++sizeChangeCnt}")
        while (layout.height > height && isFinishCutWidth.not()) {
            isNeedReadMoreText = true
            text = deleteLastLine()
        }
        if (isNeedReadMoreText) {
            attachReadMoreText()
        }
    }

    /**
     * 열 제거 로직
     * width 방식
     *      lastLineWidth >= layout.width -> 글자 제거
     *      정밀도가 떨어지는데 이유를 모르겠음 (textViewStyle을 적용하지 않아서일수도.)
     *      또, padding, margin 등도 고려해야 함
     * lineCount 방식
     *      currentLineCount >= originLineCount -> 글자 제거
     *      setText를 반복호출하여 UI Thread에서 반복이 일어나므로 최적화 필요 -> 본문 텍스트 - 사이즈 차이가 큰 경우 ANR 위험
     *      padding, margin 등 고려 안 해도 됨
     * */
    private fun attachReadMoreText() {
        if (originLineCount == 0) originLineCount = lineCount
        if (text.toString() == collapseText.toString()) {
            return
        }
        if (this::truncatedOriginText.isInitialized.not()) {
            truncatedOriginText = StringBuffer(text)
        }
        do {
            collapseText = getCollapseText()
            text = collapseText

            with(truncatedOriginText) {
                if (isBlank()) return
                deleteCharAt(lastIndex)
            }
//            printSetText("firstLineCount: ${firstLineCount} curLineCount${lineCount}")
        } while (originLineCount != lineCount)
        isFinishCutWidth = true
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        printDraw(" ${++drawCnt}")
    }

    override fun setText(text: CharSequence?, type: TextView.BufferType?) {
        super.setText(text, type)
        printSetText("${++setTextCnt}")
    }

    /*
    * ===============================================================
    *                     private method
    * ===============================================================
    * */

    private fun getStyleableAttrs(attrs: AttributeSet?) {
        context.obtainStyledAttributes(attrs, R.styleable.ReadMoreTextView).run {
            readMoreText = getString(R.styleable.ReadMoreTextView_readMoreText) ?: ""
            readMoreTextColor = getColor(R.styleable.ReadMoreTextView_readMoreColor, ContextCompat.getColor(context, R.color.purple_200))
            recycle()
        }
    }

    private fun getCollapseText(): CharSequence {
        return buildSpannedString {
            append(truncatedOriginText)

            color(readMoreTextColor) {
                bold {
                    append(readMoreText)
                }
            }
        }
    }

    private fun deleteLastLine(): String {
        val lastLineStartIdx = layout.getLineStart(lineCount - 1)
        return text.substring(0, lastLineStartIdx)
    }

    /*
    * ===============================================================
    *                           Logging
    * ===============================================================
    * */

    private fun printMeasure(text: String) {
        Log.e(TAG, "onMeasure ---- $text")
    }

    private fun printDraw(text: String) {
        Log.e(TAG, "onDraw ---- $text")
    }

    private fun printSetText(text: String) {
        Log.e(TAG, "setText ---- $text")
    }

    private fun printOnSizeChanged(text: String) {
        Log.e(TAG, "onSizeChanged ---- $text")
    }

    private fun printOnLayout(text: String) {
        Log.e(TAG, "onLayout ---- $text")
    }

    private fun getMode(mode: Int): String {
        return when (mode) {
            View.MeasureSpec.AT_MOST -> "AT_MOST"
            MeasureSpec.EXACTLY -> "EXACTLY"
            MeasureSpec.UNSPECIFIED -> "UNSPECIFIED"
            else -> ""
        }
    }
}