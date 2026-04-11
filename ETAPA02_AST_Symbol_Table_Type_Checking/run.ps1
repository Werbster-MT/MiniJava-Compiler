$dir = $PSScriptRoot

# Roda todos os .mj válidos
Get-ChildItem "$dir\testes\semantico_valido_*.mj" | ForEach-Object {
    Write-Host "`n──── $($_.Name) ────" -ForegroundColor Cyan
    java -cp ".;C:\antlr\antlr-4.13.2-complete.jar" Main $_.FullName
}

# Roda todos os .mj inválidos
Get-ChildItem "$dir\testes\semantico_invalido_*.mj" | ForEach-Object {
    Write-Host "`n──── $($_.Name) ────" -ForegroundColor Cyan
    java -cp ".;C:\antlr\antlr-4.13.2-complete.jar" Main $_.FullName
}
