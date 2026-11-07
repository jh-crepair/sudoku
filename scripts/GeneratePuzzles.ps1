# PowerShell Sudoku Puzzle Generator
# Generates 180 valid puzzles and writes to puzzles.json

param()

function Get-BoxDims($size) {
    switch ($size) {
        4 { return @(2, 2) }
        6 { return @(2, 3) }
        default { return @(3, 3) }
    }
}

function Test-Valid($grid, $row, $col, $num, $size) {
    # Row check
    if ($grid[$row] -contains $num) { return $false }
    # Col check
    for ($r = 0; $r -lt $size; $r++) {
        if ($grid[$r][$col] -eq $num) { return $false }
    }
    # Box check
    $dims = Get-BoxDims $size
    $br = $dims[0]; $bc = $dims[1]
    $sr = [int]($row / $br) * $br
    $sc = [int]($col / $bc) * $bc
    for ($r = $sr; $r -lt $sr + $br; $r++) {
        for ($c = $sc; $c -lt $sc + $bc; $c++) {
            if ($grid[$r][$c] -eq $num) { return $false }
        }
    }
    return $true
}

function Solve-Grid($grid, $size, $shuffle = $false) {
    for ($row = 0; $row -lt $size; $row++) {
        for ($col = 0; $col -lt $size; $col++) {
            if ($grid[$row][$col] -eq 0) {
                $nums = 1..$size
                if ($shuffle) { $nums = $nums | Sort-Object { Get-Random } }
                foreach ($num in $nums) {
                    if (Test-Valid $grid $row $col $num $size) {
                        $grid[$row][$col] = $num
                        if (Solve-Grid $grid $size $shuffle) { return $true }
                        $grid[$row][$col] = 0
                    }
                }
                return $false
            }
        }
    }
    return $true
}

function Count-Solutions($grid, $size) {
    for ($row = 0; $row -lt $size; $row++) {
        for ($col = 0; $col -lt $size; $col++) {
            if ($grid[$row][$col] -eq 0) {
                $count = 0
                for ($num = 1; $num -le $size; $num++) {
                    if (Test-Valid $grid $row $col $num $size) {
                        $grid[$row][$col] = $num
                        # Deep copy
                        $copy = $grid | ForEach-Object { $_.Clone() }
                        $count += Count-Solutions $copy $size
                        $grid[$row][$col] = 0
                        if ($count -ge 2) { return $count }
                    }
                }
                return $count
            }
        }
    }
    return 1
}

function New-FullGrid($size) {
    $grid = @()
    for ($r = 0; $r -lt $size; $r++) {
        $grid += ,@(0) * $size  # Initialize with zeros
        $grid[$r] = [int[]]@(0) * $size
    }
    Solve-Grid $grid $size $true | Out-Null
    return $grid
}

function New-Puzzle($size, $difficulty, $orderIndex) {
    $revealCounts = @{
        4 = @{ EASY = 10; MEDIUM = 7; HARD = 5 }
        6 = @{ EASY = 20; MEDIUM = 15; HARD = 11 }
        9 = @{ EASY = 40; MEDIUM = 31; HARD = 22 }
    }
    $targetReveal = $revealCounts[$size][$difficulty]
    $totalCells = $size * $size

    $attempts = 0
    while ($true) {
        $attempts++
        $full = New-FullGrid $size
        $solution = @()
        for ($r = 0; $r -lt $size; $r++) {
            for ($c = 0; $c -lt $size; $c++) {
                $solution += $full[$r][$c]
            }
        }

        # Create working copy
        $work = $full | ForEach-Object { [int[]]$_.Clone() }

        # Randomly remove cells
        $indices = 0..($totalCells - 1) | Sort-Object { Get-Random }
        $given = [System.Collections.Generic.HashSet[int]](0..($totalCells - 1))
        $removed = 0
        $toRemove = $totalCells - $targetReveal

        foreach ($idx in $indices) {
            if ($removed -ge $toRemove) { break }
            $r = [int]($idx / $size)
            $c = $idx % $size
            $backup = $work[$r][$c]
            $work[$r][$c] = 0
            
            # Check uniqueness with a copy
            $testGrid = $work | ForEach-Object { [int[]]$_.Clone() }
            $sols = Count-Solutions $testGrid $size
            
            if ($sols -eq 1) {
                $given.Remove($idx) | Out-Null
                $removed++
            } else {
                $work[$r][$c] = $backup
            }
        }

        if ($removed -ge ($toRemove - 2)) {
            $givenMap = @{}
            foreach ($idx in $given) {
                $givenMap["$idx"] = $solution[$idx]
            }
            return @{
                gridSize = $size
                difficulty = $difficulty
                orderIndex = $orderIndex
                givenCells = $givenMap
                solution = $solution
            }
        }
    }
}

function ConvertTo-PuzzleJson($puzzle) {
    $givenParts = $puzzle.givenCells.GetEnumerator() | 
        Sort-Object { [int]$_.Key } |
        ForEach-Object { "`"$($_.Key)`":$($_.Value)" }
    $givenStr = $givenParts -join ","
    $solutionStr = $puzzle.solution -join ","
    return "{`"gridSize`":$($puzzle.gridSize),`"difficulty`":`"$($puzzle.difficulty)`",`"orderIndex`":$($puzzle.orderIndex),`"givenCells`":{$givenStr},`"solution`":[$solutionStr]}"
}

# ── Main ──────────────────────────────────────────────────────────────────────
Write-Host "🧩 Sudoku Puzzle Generator — generating 180 puzzles..." -ForegroundColor Cyan

$allPuzzles = @()
$gridSizes = @(4, 6, 9)
$difficulties = @("EASY", "MEDIUM", "HARD")

foreach ($size in $gridSizes) {
    foreach ($diff in $difficulties) {
        Write-Host "`n📐 ${size}x${size} $diff" -ForegroundColor Yellow
        for ($i = 1; $i -le 20; $i++) {
            Write-Host "  Puzzle $i..." -NoNewline
            $puzzle = New-Puzzle $size $diff $i
            $allPuzzles += $puzzle
            Write-Host " ✓ ($($puzzle.givenCells.Count) given)" -ForegroundColor Green
        }
    }
}

$jsonParts = $allPuzzles | ForEach-Object { ConvertTo-PuzzleJson $_ }
$json = "[\n" + ($jsonParts -join ",`n") + "`n]"

$outPath = "d:\Workspaces\1.project-workspace\sudoku\app\src\main\assets\puzzles.json"
$json | Out-File -FilePath $outPath -Encoding UTF8 -NoNewline

Write-Host "`n✅ Done! $($allPuzzles.Count) puzzles written to puzzles.json" -ForegroundColor Cyan
