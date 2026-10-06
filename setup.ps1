<#
.SYNOPSIS
  Sets up git pre-commit hooks for this repo (pre-commit framework).

.DESCRIPTION
  Installs the `pre-commit` package if missing and runs `pre-commit install`
  so the lint/format/unit-test checks run automatically on every `git commit`.
  Works on Windows PowerShell 5.1 and PowerShell 7+.
#>
$ErrorActionPreference = 'Stop'

function Test-Command($Name) {
  return $null -ne (Get-Command $Name -ErrorAction SilentlyContinue)
}

if (-not (Test-Command 'git')) {
  Write-Error 'git not found in PATH. Install Git for Windows and retry.'
  exit 1
}

if (-not (Test-Command 'python')) {
  Write-Error 'Python not found in PATH. Install Python 3 (python.org) with "Add to PATH" and retry.'
  exit 1
}

# Must run inside the repo so hooks land in .git/hooks.
$null = git rev-parse --show-toplevel 2>$null
if ($LASTEXITCODE -ne 0) {
  Write-Error 'Not inside a git repository. Run this script from the repo root.'
  exit 1
}

python -m pre_commit --version > $null 2>&1
if ($LASTEXITCODE -ne 0) {
  Write-Host 'pre-commit not found, installing via pip...'
  python -m pip install --user pre-commit
}

# Use `python -m` so it works even if pip --user Scripts dir is not on PATH.
python -m pre_commit install
Write-Host ''
Write-Host 'Done. Hooks will run on every commit.'
Write-Host 'To check everything right now: pre-commit run --all-files'
