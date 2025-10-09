README

Данный бот предоставляет функции для парсинга и просмотра страницы колледжа КС ПГУТИ, также позволяет создавать директории и фалйы на ПК где запущен испольняемый файл.

Запускайте jar файл и указывайте путь к файлу конфига. Например: config.properties

config.properties должен содержать следующие атрибуты:

bot_token =ваш_токен

bot_name =имя_юота

path =путь_где_будут_хранится файлы и директории

duration =время_обновления_расписания(в минутах)

databaseURL =jdbc:mysql://localhost:3306/имя_базы_данных

user =пользовательБазыДанных

pass =парольПользователяБД

delimiter =разделитель в файловой системе '\' windows, '/' linux

extensions =docx,doc,txt,pdf,rtf,odt,html,epub,xls,xlsx,csv,ppt,pptx,odp,pdf,jpeg,png,gif,webp,tiff,raw

fileMaxSize =30 - макс размер загружаемого файла в мб
