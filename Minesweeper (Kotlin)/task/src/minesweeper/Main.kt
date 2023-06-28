package minesweeper

import kotlin.random.Random

const val EXIT = "exit"

enum class CellStates(val symbol: String) {
    UNEXPLORED("."), EMPTY_REVEALED("/"), MINE_HIDDEN("."), MINE_REVEALED("X"), MINE_MARKED("*"), HINT(
        ""
    );
}

enum class MoveTypes(val value: String) {
    MARK_MINE("mine"), EXPLORE_CELL("free")
}

enum class GameStateValues {
    NORMAL, STEP_ON_MINE, WIN, MARKED_HINT_CELL, NO_SUCH_MOVE
}

fun main() {
    Game()
}

class Game {
    val MINEFIELD_SIZE = 9
    val MINEFIELD_EDGE = MINEFIELD_SIZE - 1
    var numberOfMines = 0
    val minefield = mutableListOf<MutableList<Cell>>()
    var minesFound = 0
    var minesMarked = 0
    val minePositions = mutableListOf<Coordinates>()

    init {
        print("How many mines do you want on the field? ")
        emptyMinefieldInit()
        numberOfMines = readln().toInt()
        addMinesRandomly(numberOfMines)
        printMinefield()
        gameLoop()
    }

    private fun gameLoop() {
        while (true) {
            print("Set/delete mines marks (x and y coordinates): ")
            val coordinates = readln().split(" ")
            if (coordinates[0] == EXIT) break;
            val output: GameStateValues = makeAMove(coordinates)
            if (output != GameStateValues.MARKED_HINT_CELL) {
                printMinefield()
            }
            if (output == GameStateValues.STEP_ON_MINE) {
                println("You stepped on a mine and failed!")
                return
            }
            if (minesFound == numberOfMines && minesFound == minesMarked) {
                println("Congratulations! You found all the mines!")
                return
            }
        }

    }

    private fun makeAMove(move: List<String>): GameStateValues {
        // input verification TODO
        val moveType = move[2]
        val currentCell = minefield[move[1].toInt() - 1][move[0].toInt() - 1]
        return when (moveType) {
            MoveTypes.MARK_MINE.value -> markUnmarkMine(currentCell)
            MoveTypes.EXPLORE_CELL.value -> exploreFreeCell(currentCell)
            else -> {
                println("No such move!")
                return GameStateValues.NO_SUCH_MOVE
            }
        }
    }

    private fun exploreFreeCell(currentCell: Cell): GameStateValues {
        when {
            (currentCell.hasMine && currentCell.state == CellStates.MINE_HIDDEN) -> {
                for (mine in minePositions) {
                    minefield[mine.x][mine.y].state = CellStates.MINE_REVEALED
                }
                return GameStateValues.STEP_ON_MINE
            }

            (currentCell.hasHintNumber() && currentCell.state == CellStates.UNEXPLORED && !currentCell.hasMine) -> {
                currentCell.state = CellStates.HINT
            }

            (!currentCell.hasHintNumber() && currentCell.state == CellStates.UNEXPLORED && !currentCell.hasMine) -> {
                currentCell.state = CellStates.EMPTY_REVEALED
                revealAllCellsAroundExploredFreeCell(currentCell)
            }

            (currentCell.state != CellStates.UNEXPLORED) -> println("minesweeper.Cell already explored or marked as mine!")
        }
        return GameStateValues.NORMAL
    }

    private fun revealAllCellsAroundExploredFreeCell(currentCell: Cell) {
        val startCoordinate =
            Coordinates(currentCell.coordinates.x - 1, currentCell.coordinates.y - 1)
        val endCoordinate =
            Coordinates(currentCell.coordinates.x + 1, currentCell.coordinates.y + 1)
        for (y in startCoordinate.y..endCoordinate.y) {
            for (x in startCoordinate.x..endCoordinate.x) {
                if (x < 0 || y < 0 || x > MINEFIELD_EDGE || y > MINEFIELD_EDGE) continue
                val exploredCell = minefield[x][y]
                if (exploredCell.state == CellStates.UNEXPLORED ||
                    (exploredCell.state == CellStates.MINE_MARKED && !exploredCell.hasMine)) {
                    if (exploredCell.hasHintNumber()) {
                        exploredCell.state = CellStates.HINT
                    } else {
                        exploredCell.state = CellStates.EMPTY_REVEALED
                    }
                    revealAllCellsAroundExploredFreeCell(exploredCell)
                }
            }
        }
    }

    private fun markUnmarkMine(currentCell: Cell): GameStateValues {
        when {
            (currentCell.hasMine && currentCell.state == CellStates.MINE_HIDDEN) -> {
                minesFound++
                minesMarked++
                currentCell.state = CellStates.MINE_MARKED
            }

            (currentCell.hasMine && currentCell.state == CellStates.MINE_MARKED) -> {
                minesFound--
                minesMarked--
                currentCell.state = CellStates.MINE_HIDDEN
            }

            (!currentCell.hasMine && currentCell.state == CellStates.HINT) -> {
                println("There is a number here!")
                return GameStateValues.MARKED_HINT_CELL
            }

            (!currentCell.hasMine && currentCell.state == CellStates.UNEXPLORED) -> {
                minesMarked++
                currentCell.state = CellStates.MINE_MARKED
            }

            (!currentCell.hasMine && currentCell.state == CellStates.MINE_MARKED) -> {
                minesMarked--
                currentCell.state = CellStates.UNEXPLORED
            }

            (!currentCell.hasMine && currentCell.state == CellStates.EMPTY_REVEALED) -> {
                println("Empty space already revealed!")
            }
        }
        return GameStateValues.NORMAL
    }

    private fun emptyMinefieldInit() {
        for (row in 0 until MINEFIELD_SIZE) {
            val tmpCol = mutableListOf<Cell>()
            for (column in 0 until MINEFIELD_SIZE) {
                tmpCol.add(Cell(CellStates.UNEXPLORED, Coordinates(row, column)))
            }
            minefield.add(tmpCol)
        }
    }

    private fun printMinefield() {
        println(" │123456789│")
        println("—│—————————│")
        for ((index, row) in minefield.withIndex()) {
            print("${index + 1}|")
            for (column in row) {
                print(if (column.state == CellStates.HINT) column.neighbourMinesCount else column.state.symbol)
            }
            println("|")
        }
        println("—│—————————│")
    }

    private fun addMinesRandomly(numberOfMines: Int) {
        repeat(numberOfMines) {
            var row = Random.nextInt(0, MINEFIELD_SIZE)
            var column = Random.nextInt(0, MINEFIELD_SIZE)
            while (true) {
                if (minefield[row][column].state != CellStates.MINE_HIDDEN) {
                    minefield[row][column] =
                        Cell(CellStates.MINE_HIDDEN, Coordinates(row, column), true)
                    minePositions.add(Coordinates(row, column))
                    addHintToNeighboursOfMine(minefield, Coordinates(row, column))
                    break;
                }
                row = Random.nextInt(0, MINEFIELD_SIZE)
                column = Random.nextInt(0, MINEFIELD_SIZE)
            }
        }
    }

    private fun addHintToNeighboursOfMine(
        minefield: MutableList<MutableList<Cell>>, coordinates: Coordinates
    ) {
        val startCoordinate = Coordinates(coordinates.x - 1, coordinates.y - 1)
        val endCoordinate = Coordinates(coordinates.x + 1, coordinates.y + 1)
        for (y in startCoordinate.y..endCoordinate.y) {
            for (x in startCoordinate.x..endCoordinate.x) {
                if (x < 0 || y < 0 || x > MINEFIELD_EDGE || y > MINEFIELD_EDGE) continue
                minefield[x][y].neighbourMinesCount++
            }
        }
    }
}

data class Cell(
    var state: CellStates = CellStates.UNEXPLORED,
    val coordinates: Coordinates,
    val hasMine: Boolean = false
) {
    var neighbourMinesCount: Int = 0

    fun hasHintNumber() = neighbourMinesCount > 0
}

data class Coordinates(val x: Int, val y: Int)


