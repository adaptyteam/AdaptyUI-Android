package com.adapty.ui.internal

import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet

internal fun ConstraintLayout.applyConstraints(block: Constraints.() -> Unit) {
    addConstraints(block)
        .also { constraintSet -> constraintSet.applyTo(this) }
}

internal fun ConstraintLayout.addConstraints(block: Constraints.() -> Unit): ConstraintSet {
    return addConstraints(ConstraintSet().apply { clone(this@addConstraints) }, block)
}

internal fun addConstraints(
    constraintSet: ConstraintSet,
    block: Constraints.() -> Unit,
): ConstraintSet {
    val constraints = Constraints().apply(block)

    with(constraintSet) {
        constraints.dimensions.forEach { (viewId, value, type) ->
            when (type) {
                Constraints.Dimensions.Type.WIDTH -> constrainWidth(viewId, value)
                Constraints.Dimensions.Type.HEIGHT -> constrainHeight(viewId, value)
            }
        }

        constraints.connections.forEach { (item1, item2, margin, goneMargin) ->
            if (margin != null) {
                connect(item1.viewId, item1.side, item2.viewId, item2.side, margin)
            } else {
                connect(item1.viewId, item1.side, item2.viewId, item2.side)
            }
            if (goneMargin != null) {
                setGoneMargin(item1.viewId, item1.side, goneMargin)
            }
        }

        constraints.chains.forEach { (edgeItem1, edgeItem2, style, chainIds, orientation, edgeMargin) ->
            when (orientation) {
                Constraints.Orientation.HORIZONTAL -> {
                    createHorizontalChainRtl(
                        edgeItem1.viewId,
                        edgeItem1.side,
                        edgeItem2.viewId,
                        edgeItem2.side,
                        chainIds,
                        null,
                        style,
                    )
                }
                Constraints.Orientation.VERTICAL -> {
                    createVerticalChain(
                        edgeItem1.viewId,
                        edgeItem1.side,
                        edgeItem2.viewId,
                        edgeItem2.side,
                        chainIds,
                        null,
                        style,
                    )
                }
            }

            if (edgeMargin != null) {
                constraintSet.setMargin(chainIds.first(), edgeItem1.side, edgeMargin)
                constraintSet.setMargin(chainIds.last(), edgeItem2.side, edgeMargin)
            }
        }
    }

    return constraintSet
}

internal class Constraints(
    val connections: MutableList<Connection> = mutableListOf(),
    val dimensions: MutableList<Dimensions> = mutableListOf(),
    val chains: MutableList<Chain> = mutableListOf(),
) {

    infix fun Int.constrainWidth(value: Int): Dimensions {
        return constrain(this, value, Dimensions.Type.WIDTH)
    }

    infix fun Int.constrainHeight(value: Int): Dimensions {
        return constrain(this, value, Dimensions.Type.HEIGHT)
    }

    infix fun Dimensions.constrainWidth(value: Int): Dimensions {
        return constrain(this.viewId, value, Dimensions.Type.WIDTH)
    }

    infix fun Dimensions.constrainHeight(value: Int): Dimensions {
        return constrain(this.viewId, value, Dimensions.Type.HEIGHT)
    }

    infix fun Int.constrainBoth(value: Int) {
        constrain(this, value, Dimensions.Type.WIDTH)
        constrain(this, value, Dimensions.Type.HEIGHT)
    }

    private fun constrain(viewId: Int, value: Int, type: Dimensions.Type): Dimensions {
        return Dimensions(viewId, value, type)
            .also(dimensions::add)
    }

    class Dimensions(
        val viewId: Int,
        val value: Int,
        val type: Type,
    ) {
        operator fun component1() = viewId
        operator fun component2() = value
        operator fun component3() = type

        enum class Type { WIDTH, HEIGHT }
    }

    infix fun Int.of(viewId: Int): Connection.Builder {
        return Connection.Builder(this@Constraints, Connection.Item(viewId, this))
    }

    infix fun Connection.Builder.to(otherSide: Int): Connection.Builder {
        this.items.add(Connection.Item(side = otherSide))
        return this
    }

    infix fun Connection.Builder.of(viewId: Int): Connection {
        val (item1, item2) = items
        item2.viewId = viewId
        return Connection(item1, item2)
            .also { connection -> constraints.connections.add(connection) }
    }

    infix fun Connection.margin(margin: Int): Connection {
        return apply { this.margin = margin }
    }

    infix fun Connection.goneMargin(goneMargin: Int): Connection {
        return apply { this.goneMargin = goneMargin }
    }

    enum class Orientation { VERTICAL, HORIZONTAL }

    infix fun Int.horizontallyTo(otherId: Int): HorizontalOrVerticalConnection {
        return horizontallyOrVerticallyTo(this, otherId, Orientation.HORIZONTAL)
    }

    infix fun Int.verticallyTo(otherId: Int): HorizontalOrVerticalConnection {
        return horizontallyOrVerticallyTo(this, otherId, Orientation.VERTICAL)
    }

    private fun horizontallyOrVerticallyTo(
        thisId: Int,
        otherId: Int,
        type: Orientation,
    ): HorizontalOrVerticalConnection {
        val item1: Connection.Item
        val item2: Connection.Item
        val item3: Connection.Item
        val item4: Connection.Item
        when (type) {
            Orientation.HORIZONTAL -> {
                item1 = Connection.Item(thisId, ConstraintSet.START)
                item2 = Connection.Item(otherId, ConstraintSet.START)
                item3 = Connection.Item(thisId, ConstraintSet.END)
                item4 = Connection.Item(otherId, ConstraintSet.END)
            }
            Orientation.VERTICAL -> {
                item1 = Connection.Item(thisId, ConstraintSet.TOP)
                item2 = Connection.Item(otherId, ConstraintSet.TOP)
                item3 = Connection.Item(thisId, ConstraintSet.BOTTOM)
                item4 = Connection.Item(otherId, ConstraintSet.BOTTOM)
            }
        }
        return HorizontalOrVerticalConnection(Connection(item1, item2), Connection(item3, item4))
            .also {
                connections.add(it.connection1)
                connections.add(it.connection2)
            }
    }

    infix fun HorizontalOrVerticalConnection.margins(margin: Int): HorizontalOrVerticalConnection {
        return margin1(margin).margin2(margin)
    }

    infix fun HorizontalOrVerticalConnection.margin1(margin: Int): HorizontalOrVerticalConnection {
        return apply { this.connection1.margin = margin }
    }

    infix fun HorizontalOrVerticalConnection.margin2(margin: Int): HorizontalOrVerticalConnection {
        return apply { this.connection2.margin = margin }
    }

    infix fun HorizontalOrVerticalConnection.goneMargins(goneMargin: Int): HorizontalOrVerticalConnection {
        return goneMargin1(goneMargin).goneMargin2(goneMargin)
    }

    infix fun HorizontalOrVerticalConnection.goneMargin1(goneMargin: Int): HorizontalOrVerticalConnection {
        return apply { this.connection1.goneMargin = goneMargin }
    }

    infix fun HorizontalOrVerticalConnection.goneMargin2(goneMargin: Int): HorizontalOrVerticalConnection {
        return apply { this.connection2.goneMargin = goneMargin }
    }

    class HorizontalOrVerticalConnection(
        val connection1: Connection,
        val connection2: Connection,
    )

    infix fun Connection.Builder.chain(style: Int): Chain.Builder {
        return Chain.Builder(this, style)
    }

    infix fun Chain.Builder.with(viewIds: IntArray): Chain.Builder {
        return apply { this.chainIds = viewIds }
    }

    infix fun Chain.Builder.to(otherEdgeSide: Int): Chain.Builder {
        return apply { this.edgeItems.add(Connection.Item(side = otherEdgeSide)) }
    }

    infix fun Chain.Builder.of(viewId: Int): Chain {
        val (edgeItem1, edgeItem2) = edgeItems
        edgeItem2.viewId = viewId
        return Chain(edgeItem1, edgeItem2, style, chainIds)
            .also { chain -> constraints.chains.add(chain) }
    }

    infix fun Chain.edgeMargins(margin: Int): Chain {
        return apply { this.edgeMargin = margin }
    }

    val Constraints.spread get() = ConstraintSet.CHAIN_SPREAD
    val Constraints.spreadInside get() = ConstraintSet.CHAIN_SPREAD_INSIDE
    val Constraints.packed get() = ConstraintSet.CHAIN_PACKED

    class Chain(
        val edgeItem1: Connection.Item,
        val edgeItem2: Connection.Item,
        val style: Int,
        val chainIds: IntArray,
    ) {
        val orientation: Orientation =
            if (edgeItem1.side in listOf(ConstraintSet.TOP, ConstraintSet.BOTTOM))
                Orientation.VERTICAL
            else
                Orientation.HORIZONTAL

        var edgeMargin: Int? = null

        operator fun component1() = edgeItem1
        operator fun component2() = edgeItem2
        operator fun component3() = style
        operator fun component4() = chainIds
        operator fun component5() = orientation
        operator fun component6() = edgeMargin

        class Builder(connectionBuilder: Connection.Builder, val style: Int) {
            val constraints: Constraints = connectionBuilder.constraints
            val edgeItems = connectionBuilder.items
            var chainIds = intArrayOf()
        }
    }

    class Connection(
        val item1: Item,
        val item2: Item,
    ) {
        var margin: Int? = null
        var goneMargin: Int? = null

        operator fun component1() = item1
        operator fun component2() = item2
        operator fun component3() = margin
        operator fun component4() = goneMargin

        class Builder(
            val constraints: Constraints,
            item1: Item,
        ) {
            val items = mutableListOf(item1)
            var margin: Int? = null
            var goneMargin: Int? = null
        }

        class Item(
            var viewId: Int = 0,
            var side: Int = 0,
        )
    }
}