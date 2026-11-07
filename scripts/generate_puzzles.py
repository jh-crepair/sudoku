"""
Sudoku Puzzle Generator for Kids Sudoku App
Generates 180 valid puzzles (20 per gridSize x difficulty combination)
Output: app/src/main/assets/puzzles.json

Optimized version: uses dig-based removal with limited backtrack uniqueness check.
"""

import json
import random
import copy
import os
import sys

# --- Solver helpers ---

def box_dims(size):
    if size == 4: return 2, 2
    if size == 6: return 2, 3
    return 3, 3


def is_valid(board, row, col, num, size, br, bc):
    if num in board[row]: return False
    for r in range(size):
        if board[r][col] == num: return False
    sr = (row // br) * br
    sc = (col // bc) * bc
    for r in range(sr, sr + br):
        for c in range(sc, sc + bc):
            if board[r][c] == num: return False
    return True


def solve_one(board, size, br, bc):
    """Fills board in-place. Returns True if solvable."""
    for row in range(size):
        for col in range(size):
            if board[row][col] == 0:
                nums = list(range(1, size + 1))
                random.shuffle(nums)
                for num in nums:
                    if is_valid(board, row, col, num, size, br, bc):
                        board[row][col] = num
                        if solve_one(board, size, br, bc):
                            return True
                        board[row][col] = 0
                return False
    return True


def has_unique_solution(board, size, br, bc):
    """Returns True if puzzle has exactly one solution (stops after finding 2)."""
    count = [0]

    def bt(bd):
        for row in range(size):
            for col in range(size):
                if bd[row][col] == 0:
                    for num in range(1, size + 1):
                        if is_valid(bd, row, col, num, size, br, bc):
                            bd[row][col] = num
                            bt(bd)
                            if count[0] >= 2:
                                bd[row][col] = 0
                                return
                            bd[row][col] = 0
                    return
        count[0] += 1

    bt([row[:] for row in board])
    return count[0] == 1


def generate_full_board(size):
    br, bc = box_dims(size)
    board = [[0] * size for _ in range(size)]
    solve_one(board, size, br, bc)
    return board


def make_puzzle(full_board, size, num_given):
    """Remove cells to reach num_given clues, ensuring unique solution."""
    br, bc = box_dims(size)
    board = [row[:] for row in full_board]
    cells = [(r, c) for r in range(size) for c in range(size)]
    random.shuffle(cells)

    total = size * size
    target_remove = total - num_given

    removed = 0
    for (r, c) in cells:
        if removed >= target_remove:
            break
        val = board[r][c]
        board[r][c] = 0
        if has_unique_solution(board, size, br, bc):
            removed += 1
        else:
            board[r][c] = val  # restore

    return board


def board_to_given_cells(puzzle, size):
    return {str(r * size + c): puzzle[r][c]
            for r in range(size) for c in range(size) if puzzle[r][c] != 0}


def board_to_flat(board, size):
    return [board[r][c] for r in range(size) for c in range(size)]


# Configuration
CONFIGS = [
    (4, "EASY",   10),
    (4, "MEDIUM",  7),
    (4, "HARD",    5),
    (6, "EASY",   20),
    (6, "MEDIUM", 15),
    (6, "HARD",   11),
    (9, "EASY",   40),
    (9, "MEDIUM", 31),
    (9, "HARD",   22),
]

PUZZLES_PER_COMBO = 20


def generate_all():
    puzzles = []
    for grid_size, difficulty, num_given in CONFIGS:
        print(f"Generating {PUZZLES_PER_COMBO}x {grid_size}x{grid_size} {difficulty} (given={num_given})...", flush=True)
        for order_index in range(1, PUZZLES_PER_COMBO + 1):
            full = generate_full_board(grid_size)
            puzzle = make_puzzle(full, grid_size, num_given)
            given_cells = board_to_given_cells(puzzle, grid_size)
            solution = board_to_flat(full, grid_size)
            puzzles.append({
                "gridSize": grid_size,
                "difficulty": difficulty,
                "orderIndex": order_index,
                "givenCells": given_cells,
                "solution": solution
            })
            sys.stdout.write(f"  {order_index}/{PUZZLES_PER_COMBO} done (clues={len(given_cells)})\n")
            sys.stdout.flush()
    return puzzles


if __name__ == "__main__":
    random.seed(12345)
    out = os.path.join(
        os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
        "app", "src", "main", "assets", "puzzles.json"
    )
    os.makedirs(os.path.dirname(out), exist_ok=True)
    all_puzzles = generate_all()
    with open(out, "w", encoding="utf-8") as f:
        json.dump(all_puzzles, f, separators=(",", ":"))
    print(f"\nDone! {len(all_puzzles)} puzzles -> {out}", flush=True)
