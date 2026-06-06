$antlr = "C:\antlr\antlr-4.13.2-complete.jar"
$dir = $PSScriptRoot
$etapa2 = Join-Path $dir "..\ETAPA02_AST_Symbol_Table_Type_Checking"

New-Item -ItemType Directory -Force -Path "$dir\parser" | Out-Null
java -jar $antlr -visitor -package parser -o "$dir\parser" "$etapa2\MiniJava.g4"

$sources = @()
$sources += (Get-ChildItem "$dir\parser\*.java").FullName
$sources += (Get-ChildItem "$etapa2\syntaxtree\*.java").FullName
$sources += "$etapa2\visitor\BuildASTVisitor.java"
$sources += "$etapa2\visitor\TypeCheckVisitor.java"
$sources += "$etapa2\visitor\TypeDepthFirstVisitor.java"
$sources += "$etapa2\visitor\TypeVisitor.java"
$sources += "$etapa2\visitor\Visitor.java"
$sources += (Get-ChildItem "$etapa2\symboltable\*.java").FullName
$sources += (Get-ChildItem "$dir\activation records\*.java").FullName
$sources += (Get-ChildItem "$dir\IR Tree\*.java").FullName
$sources += (Get-ChildItem "$dir\basic blocks\*.java").FullName
$sources += (Get-ChildItem "$dir\Symbol\*.java").FullName
$sources += (Get-ChildItem "$dir\frame\*.java").FullName
$sources += (Get-ChildItem "$dir\mips\*.java").FullName
$sources += (Get-ChildItem "$dir\Assem\*.java").FullName
$sources += "$dir\visitor\IRGenVisitor.java"
$sources += "$dir\Main.java"

javac -d . -cp ".;$antlr;$etapa2" $sources

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build ETAPA04 concluido com sucesso!" -ForegroundColor Green
} else {
    Write-Host "Erros na compilacao da ETAPA04." -ForegroundColor Red
}
