# build.ps1 — ETAPA02: gera parser ANTLR e compila tudo
$antlr = "C:\antlr\antlr-4.13.2-complete.jar"
$dir   = $PSScriptRoot

# 1. Gerar parser com suporte a Visitor no pacote 'parser'
New-Item -ItemType Directory -Force -Path "$dir\parser" | Out-Null
java -jar $antlr -visitor -package parser -o "$dir\parser" "$dir\MiniJava.g4"

# 2. Compilar todos os fontes
javac -cp ".;$antlr" `
    parser\*.java `
    syntaxtree\*.java `
    visitor\*.java `
    symboltable\*.java `
    Main.java

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build ETAPA02 concluido com sucesso!" -ForegroundColor Green
} else {
    Write-Host "Erros na compilacao." -ForegroundColor Red
}

