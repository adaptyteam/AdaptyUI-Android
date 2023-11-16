package com.adapty.ui.internal

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.annotation.RestrictTo
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.ConstraintSet.BOTTOM
import androidx.constraintlayout.widget.ConstraintSet.CHAIN_SPREAD
import androidx.constraintlayout.widget.ConstraintSet.END
import androidx.constraintlayout.widget.ConstraintSet.MATCH_CONSTRAINT
import androidx.constraintlayout.widget.ConstraintSet.MATCH_CONSTRAINT_WRAP
import androidx.constraintlayout.widget.ConstraintSet.PARENT_ID
import androidx.constraintlayout.widget.ConstraintSet.START
import androidx.constraintlayout.widget.ConstraintSet.TOP
import androidx.constraintlayout.widget.ConstraintSet.WRAP_CONTENT

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class LayoutHelper {

    fun constrain(
        viewId: Int,
        widthConstraint: Int,
        heightConstraint: Int,
        verticalAnchor: ViewAnchor,
        constraintSet: ConstraintSet,
        horizontalMargin: Int = 0,
    ) {
        constrain(
            viewId,
            widthConstraint,
            heightConstraint,
            null,
            null,
            verticalAnchor,
            constraintSet,
            horizontalMargin,
        )
    }

    fun constrain(
        viewId: Int,
        widthConstraint: Int,
        heightConstraint: Int,
        widthConstraintDefault: Int?,
        heightConstraintDefault: Int?,
        verticalAnchor: ViewAnchor,
        constraintSet: ConstraintSet,
        horizontalMargin: Int = 0,
    ) {
        with(constraintSet) {
            constrainWidth(viewId, widthConstraint)
            constrainHeight(viewId, heightConstraint)
            widthConstraintDefault?.let {
                constrainDefaultWidth(viewId, widthConstraintDefault)
            }
            heightConstraintDefault?.let {
                constrainDefaultHeight(viewId, heightConstraintDefault)
            }
            connect(viewId, START, PARENT_ID, START, horizontalMargin)
            connect(viewId, END, PARENT_ID, END, horizontalMargin)
            connect(viewId, verticalAnchor.otherSide, verticalAnchor.view.id, verticalAnchor.side, verticalAnchor.margin)
        }
    }

    fun constrain(
        complexButton: ComplexButton,
        widthConstraint: Int,
        heightConstraint: Int,
        verticalAnchor: ViewAnchor,
        constraintSet: ConstraintSet,
        horizontalMargin: Int = 0,
    ) {
        val bgViewId = complexButton.bgView.id
        with(constraintSet) {
            constrainWidth(bgViewId, widthConstraint)
            constrainHeight(bgViewId, heightConstraint)
            connect(bgViewId, START, PARENT_ID, START, horizontalMargin)
            connect(bgViewId, END, PARENT_ID, END, horizontalMargin)
            connect(
                bgViewId,
                verticalAnchor.otherSide,
                verticalAnchor.view.id,
                verticalAnchor.side,
                verticalAnchor.margin
            )

            if (complexButton.textView != null) {
                val textViewId = complexButton.textView.id

                val paddings = complexButton.paddings

                constrainWidth(textViewId, MATCH_CONSTRAINT)
                constrainDefaultWidth(textViewId, MATCH_CONSTRAINT_WRAP)
                constrainHeight(textViewId, MATCH_CONSTRAINT)
                constrainDefaultHeight(textViewId, MATCH_CONSTRAINT_WRAP)
                connect(textViewId, START, bgViewId, START, paddings.start)
                connect(textViewId, END, bgViewId, END, paddings.end)
                connect(textViewId, TOP, bgViewId, TOP, paddings.top)
                connect(textViewId, BOTTOM, bgViewId, BOTTOM, paddings.bottom)
            }
        }
    }

    fun constrainFeatureViews(
        featureUIBlock: FeatureUIBlock,
        verticalAnchor: ViewAnchor,
        verticalSpacing: Int,
        edgeMarginHorizontal: Int,
        constraintSet: ConstraintSet,
        templateConfig: TemplateConfig,
    ) {
        with(constraintSet) {
            when (featureUIBlock) {
                is FeatureUIBlock.List -> {
                    val textViewId = featureUIBlock.textView.id

                    constrainWidth(textViewId, MATCH_CONSTRAINT)
                    constrainHeight(textViewId, WRAP_CONTENT)

                    connect(textViewId, START, PARENT_ID, START, edgeMarginHorizontal)
                    connect(textViewId, END, PARENT_ID, END, edgeMarginHorizontal)

                    connect(
                        textViewId,
                        verticalAnchor.otherSide,
                        verticalAnchor.view.id,
                        verticalAnchor.side,
                        verticalAnchor.margin,
                    )

                    verticalAnchor.update(featureUIBlock.textView, verticalAnchor.side, verticalSpacing)
                }
                is FeatureUIBlock.TimeLine -> {
                    val featureCells =
                        if (templateConfig.renderDirection == TemplateConfig.RenderDirection.TOP_TO_BOTTOM) featureUIBlock.entries else featureUIBlock.entries.reversed()

                    featureCells.forEachIndexed { i, cell ->
                        val imageViewId = cell.imageView.id
                        val textViewId = cell.textView.id

                        constrainWidth(imageViewId, cell.drawableWidthPx)
                        constrainHeight(imageViewId, MATCH_CONSTRAINT)

                        constrainWidth(textViewId, MATCH_CONSTRAINT)
                        constrainHeight(textViewId, WRAP_CONTENT)

                        connect(imageViewId, START, PARENT_ID, START, edgeMarginHorizontal)
                        connect(textViewId, START, imageViewId, END, cell.textStartMarginPx)
                        connect(textViewId, END, PARENT_ID, END, edgeMarginHorizontal)

                        connect(imageViewId, TOP, textViewId, TOP)
                        connect(imageViewId, BOTTOM, textViewId, BOTTOM)

                        connect(
                            textViewId,
                            verticalAnchor.otherSide,
                            verticalAnchor.view.id,
                            verticalAnchor.side,
                            verticalAnchor.margin,
                        )

                        verticalAnchor.update(cell.textView, verticalAnchor.side, verticalSpacing)
                    }
                }
            }
        }
    }

    fun constrainProductCells(
        context: Context,
        productCells: List<View>,
        blockType: Products.BlockType.Multiple,
        verticalAnchor: ViewAnchor,
        edgeMarginHorizontal: Int,
        fromTopToBottom: Boolean,
        constraintSet: ConstraintSet,
    ) {
        val marginBetween =
            PRODUCT_CELL_VERTICAL_SPACING_DP.dp(context).toInt()

        with(constraintSet) {
            when (blockType) {
                Products.BlockType.Horizontal -> {
                    val productCellHeight = PRODUCT_CELL_HORIZONTAL_HEIGHT_DP.dp(context).toInt()

                    if (productCells.size == 1) {
                        val viewId = productCells.first().id

                        constrainWidth(viewId, MATCH_CONSTRAINT)
                        constrainHeight(viewId, productCellHeight)

                        connect(viewId, START, PARENT_ID, START, edgeMarginHorizontal)
                        connect(viewId, END, PARENT_ID, END, edgeMarginHorizontal)

                        connect(
                            viewId,
                            verticalAnchor.otherSide,
                            verticalAnchor.view.id,
                            verticalAnchor.side,
                            verticalAnchor.margin,
                        )
                    } else {
                        val chainIds = productCells.map { view ->
                            val viewId = view.id

                            constrainWidth(viewId, MATCH_CONSTRAINT)
                            constrainHeight(viewId, productCellHeight)

                            connect(
                                viewId,
                                verticalAnchor.otherSide,
                                verticalAnchor.view.id,
                                verticalAnchor.side,
                                verticalAnchor.margin,
                            )

                            viewId
                        }.toIntArray()

                        createHorizontalChainRtl(
                            PARENT_ID,
                            START,
                            PARENT_ID,
                            END,
                            chainIds,
                            chainIds.map { 1f }.toFloatArray(),
                            CHAIN_SPREAD,
                        )

                        setMargin(chainIds.first(), START, edgeMarginHorizontal)
                        setMargin(chainIds.last(), END, edgeMarginHorizontal)
                        val halfEdgeMarginHorizontal = (PRODUCT_CELL_HORIZONTAL_SPACING_DP / 2).dp(context).toInt()
                        chainIds.forEachIndexed { i, id ->
                            if (i != 0)
                                setMargin(chainIds[i], START, halfEdgeMarginHorizontal)
                            if (i != chainIds.lastIndex)
                                setMargin(chainIds[i], END, halfEdgeMarginHorizontal)
                        }
                    }

                    verticalAnchor.update(
                        productCells.first(),
                        BOTTOM,
                        PURCHASE_BUTTON_TOP_MARGIN_DP.dp(context).toInt(),
                    )
                }

                Products.BlockType.Vertical -> {
                    val productCellHeight = PRODUCT_CELL_VERTICAL_HEIGHT_DP.dp(context).toInt()

                    productCells.forEachIndexed { i, view ->
                        val viewId = view.id

                        constrainWidth(viewId, MATCH_CONSTRAINT)
                        constrainHeight(viewId, productCellHeight)

                        connect(viewId, START, PARENT_ID, START, edgeMarginHorizontal)
                        connect(viewId, END, PARENT_ID, END, edgeMarginHorizontal)

                        connect(
                            viewId,
                            verticalAnchor.otherSide,
                            verticalAnchor.view.id,
                            verticalAnchor.side,
                            verticalAnchor.margin,
                        )

                        verticalAnchor.update(
                            view,
                            if (fromTopToBottom) BOTTOM else TOP,
                            marginBetween,
                        )
                    }
                }
            }
        }
    }

    fun constrainInnerProductText(
        viewId: Int,
        anchors: List<ViewAnchor>,
        constraintSet: ConstraintSet,
    ) {
        with(constraintSet) {
            constrainWidth(viewId, MATCH_CONSTRAINT)
            constrainHeight(viewId, WRAP_CONTENT)

            anchors.forEach { anchor ->
                connect(
                    viewId,
                    anchor.otherSide,
                    anchor.view.id,
                    anchor.side,
                    anchor.margin
                )
            }
        }
    }

    fun constrainMainProductTag(
        productTagView: TextView,
        productCellViewId: Int,
        blockType: Products.BlockType.Multiple,
        constraintSet: ConstraintSet,
    ) {
        val viewId = productTagView.id

        val context = productTagView.context
        val heightPx = productTagView.textSize + (PRODUCT_TAG_VERTICAL_PADDING_DP * 2).dp(context)

        with(constraintSet) {
            constrainWidth(viewId, MATCH_CONSTRAINT)
            constrainDefaultWidth(viewId, MATCH_CONSTRAINT_WRAP)
            constrainHeight(viewId, heightPx.toInt())

            when (blockType) {
                Products.BlockType.Vertical -> {
                    val marginEndPx = PRODUCT_TAG_END_MARGIN_DP.dp(context).toInt()
                    connect(viewId, END, productCellViewId, END, marginEndPx)
                    val marginBottomPx = (PRODUCT_CELL_VERTICAL_HEIGHT_DP.dp(context) - heightPx / 2).toInt()
                    connect(viewId, BOTTOM, productCellViewId, BOTTOM, marginBottomPx)
                }
                Products.BlockType.Horizontal -> {
                    connect(viewId, START, productCellViewId, START)
                    connect(viewId, END, productCellViewId, END)
                    val marginBottomPx = (PRODUCT_CELL_HORIZONTAL_HEIGHT_DP.dp(context) - heightPx / 2).toInt()
                    connect(viewId, BOTTOM, productCellViewId, BOTTOM, marginBottomPx)
                }
            }
        }
    }

    fun constrainFooterButtons(
        footerButtons: List<View>,
        verticalAnchor: ViewAnchor,
        edgeMarginHorizontal: Int,
        constraintSet: ConstraintSet,
    ) {
        with(constraintSet) {
            if (footerButtons.size == 1) {
                val viewId = footerButtons.first().id

                constrainWidth(viewId, MATCH_CONSTRAINT)
                constrainDefaultWidth(viewId, MATCH_CONSTRAINT_WRAP)
                constrainHeight(viewId, WRAP_CONTENT)

                connect(viewId, START, PARENT_ID, START, edgeMarginHorizontal)
                connect(viewId, END, PARENT_ID, END, edgeMarginHorizontal)

                connect(
                    viewId,
                    verticalAnchor.otherSide,
                    verticalAnchor.view.id,
                    verticalAnchor.side,
                    verticalAnchor.margin,
                )
            } else {
                val chainIds = footerButtons.map { view ->
                    val viewId = view.id

                    constrainWidth(viewId, MATCH_CONSTRAINT)
                    constrainDefaultWidth(viewId, MATCH_CONSTRAINT_WRAP)
                    constrainHeight(viewId, WRAP_CONTENT)

                    connect(
                        viewId,
                        verticalAnchor.otherSide,
                        verticalAnchor.view.id,
                        verticalAnchor.side,
                        verticalAnchor.margin,
                    )

                    viewId
                }.toIntArray()

                createHorizontalChainRtl(
                    PARENT_ID,
                    START,
                    PARENT_ID,
                    END,
                    chainIds,
                    chainIds.map { 1f }.toFloatArray(),
                    CHAIN_SPREAD,
                )

                setMargin(chainIds.first(), START, edgeMarginHorizontal)
                setMargin(chainIds.last(), END, edgeMarginHorizontal)
            }
        }
    }
}