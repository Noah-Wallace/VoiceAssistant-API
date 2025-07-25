@echo off
REM Batch script to retrain OpenNLP model and deploy to resources
set NLP_BIN=D:\apache-opennlp-2.5.4-bin\apache-opennlp-2.5.4\bin
set PROJECT_RES=C:\Users\hp\Documents\NetBeansProjects\voiceassistantapi\src\main\resources

"%NLP_BIN%\opennlp.bat" DoccatTrainer -model intent-classifier.bin -lang en -data "%PROJECT_RES%\intent.train" -encoding UTF-8
if exist intent-classifier.bin move /Y intent-classifier.bin "%PROJECT_RES%\intent-classifier.bin"
echo Model retrained and deployed!
