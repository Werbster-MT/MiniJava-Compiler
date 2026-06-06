$dir = $PSScriptRoot
$antlr = "C:\antlr\antlr-4.13.2-complete.jar"

Get-ChildItem "$dir\testes\*.java" | ForEach-Object {
    Write-Host "`n──── $($_.Name) ────" -ForegroundColor Green
    java -cp ".;$antlr" Main $_.FullName
}
