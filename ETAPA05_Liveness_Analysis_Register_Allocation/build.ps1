$antlr = "C:\antlr\antlr-4.13.2-complete.jar"
$dir = $PSScriptRoot
$etapa2 = Join-Path $dir "..\ETAPA02_AST_Symbol_Table_Type_Checking"

New-Item -ItemType Directory -Force -Path "$dir\parser" | Out-Null
java -jar $antlr -visitor -package parser -o "$dir\parser" "$etapa2\MiniJava.g4"

$sources = @()
$sources += (Get-ChildItem "$dir\parser\*.java").FullName
$sources += (Get-ChildItem "$dir\syntaxtree\*.java").FullName
$sources += (Get-ChildItem "$dir\visitor\*.java").FullName
$sources += (Get-ChildItem "$dir\symboltable\*.java").FullName
$sources += (Get-ChildItem "$dir\Symbol\*.java").FullName
$sources += (Get-ChildItem "$dir\Temp\*.java").FullName
$sources += (Get-ChildItem "$dir\Util\*.java").FullName
$sources += (Get-ChildItem "$dir\Tree\*.java").FullName
$sources += (Get-ChildItem "$dir\frame\*.java").FullName
$sources += (Get-ChildItem "$dir\mips\*.java").FullName
$sources += (Get-ChildItem "$dir\Canon\*.java").FullName
$sources += (Get-ChildItem "$dir\Assem\*.java").FullName
$sources += (Get-ChildItem "$dir\graph\*.java").FullName
$sources += (Get-ChildItem "$dir\flowgraph\*.java").FullName
$sources += (Get-ChildItem "$dir\regalloc\*.java").FullName
$sources += "$dir\Main.java"

javac -d . -cp ".;$antlr" $sources

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build ETAPA05 concluido com sucesso!" -ForegroundColor Green
} else {
    Write-Host "Erros na compilacao da ETAPA05." -ForegroundColor Red
}
