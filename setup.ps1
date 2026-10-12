<#
.SYNOPSIS
  Sets up git hooks for this repo (lefthook).

.DESCRIPTION
  Installs `lefthook` if missing and runs `lefthook install`
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

# Must run inside the repo so hooks land in .git/hooks.
$null = git rev-parse --show-toplevel 2>$null
if ($LASTEXITCODE -ne 0) {
  Write-Error 'Not inside a git repository. Run this script from the repo root.'
  exit 1
}

if (-not (Test-Command 'lefthook')) {
  if (Test-Command 'winget') {
    Write-Host 'lefthook not found, installing via winget...'
    winget install --exact --id evilmartians.lefthook
  } elseif (Test-Command 'scoop') {
    Write-Host 'lefthook not found, installing via scoop...'
    scoop install lefthook
  } else {
    Write-Error 'lefthook not found in PATH. Install it (winget install evilmartians.lefthook, or scoop install lefthook) and retry.'
    exit 1
  }
}

lefthook install
Write-Host ''
Write-Host 'Done. Hooks will run on every commit.'
Write-Host 'To check everything right now: lefthook run pre-commit'
