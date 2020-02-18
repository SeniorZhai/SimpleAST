package com.discord.simpleast.sample

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.agarron.simpleast.R
import com.discord.simpleast.core.node.Node
import com.discord.simpleast.core.node.TextNode
import com.discord.simpleast.core.parser.ParseSpec
import com.discord.simpleast.core.parser.Parser
import com.discord.simpleast.core.parser.Rule
import com.discord.simpleast.core.simple.SimpleRenderer.render
import java.util.regex.Matcher
import java.util.regex.Pattern

private const val SAMPLE_TEXT = """
  Hello @123 你好 @456 哈哈 @678
  """

class MainActivity : AppCompatActivity() {

    private lateinit var resultText: TextView
    private lateinit var input: EditText

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        resultText = findViewById(R.id.result_text)
        input = findViewById(R.id.input)

        input.setText(SAMPLE_TEXT.trimIndent())

        findViewById<View>(R.id.test_btn).setOnClickListener {
            parseInput()
        }

        (findViewById<View>(R.id.result_text) as TextView).movementMethod =
            LinkMovementMethod.getInstance()
        parseInput()
    }

    private fun parseInput() {
        val renderContext = RenderContext(
            mapOf(
                "123" to "User1234",
                "456" to "User456",
                "678" to "User678"
            )
        ) {
            Log.d("---", it)
        }
        val rules = mutableListOf<Rule<RenderContext, Node<RenderContext>>>()
        rules.add(NonterminalRule())
        rules.add(UserRule())
        val parser = Parser<RenderContext, Node<RenderContext>>(true)
        parser.addRules(rules)
        findViewById<TextView>(R.id.result_text).text =
            render(SAMPLE_TEXT, parser, renderContext, 1)
    }

    data class RenderContext(val userMap: Map<String, String>, val action: (String) -> Unit)

    class NonterminalRule :
        Rule<RenderContext, Node<RenderContext>>(Pattern.compile("^@\\d+")) {

        override fun parse(
            matcher: Matcher,
            parser: Parser<RenderContext, in Node<RenderContext>>
        ): ParseSpec<RenderContext, Node<RenderContext>> {
            return ParseSpec.createTerminal(ClickNode(matcher.group()))
        }
    }

    class ClickNode(val content: String) : Node<RenderContext>() {
        override fun render(builder: SpannableStringBuilder, renderContext: RenderContext) {
            val number = content.substring(1)
            var name = renderContext.userMap[number] ?: return
            name = "@$name"
            val sp = SpannableString(name)
            val clickableSpan = object : TouchableSpan(Color.RED, Color.BLUE, false) {
                override fun onClick(widget: View) {
                    renderContext.action(number)
                }
            }

            sp.setSpan(
                clickableSpan,
                0, name.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.append(sp)
        }

        override fun toString() = "${javaClass.simpleName}[${getChildren()?.size}]: $content"
    }

    class UserRule :
        Rule<RenderContext, Node<RenderContext>>(Pattern.compile("^[\\s\\S]+?(?=[^0-9A-Za-z\\s\\u00c0-\\uffff]|\\n| {2,}\\n|\\w+:\\S|$)")) {

        override fun parse(
            matcher: Matcher,
            parser: Parser<RenderContext, in Node<RenderContext>>
        ): ParseSpec<RenderContext, Node<RenderContext>> {
            return ParseSpec.createTerminal(TextNode(matcher.group()))
        }
    }
}


