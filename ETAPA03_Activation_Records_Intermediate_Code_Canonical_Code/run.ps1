$dir = $PSScriptRoot
$antlr = "C:\antlr\antlr-4.13.2-complete.jar"
$etapa2 = Join-Path $dir "..\ETAPA02_AST_Symbol_Table_Type_Checking"

Get-ChildItem "$dir\testes\ir_valido_*.mj" | ForEach-Object {
    Write-Host "`n──── $($_.Name) ────" -ForegroundColor Cyan
    java -cp ".;$antlr;$etapa2" Main $_.FullName
}
