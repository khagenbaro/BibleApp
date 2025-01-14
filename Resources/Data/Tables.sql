CREATE TABLE IF NOT EXISTS Bible (
	bibleId BIGINT PRIMARY KEY,
	bibleName VARCHAR(100),
	bibleShortName VARCHAR(10),
	yearOfPublication VARCHAR(20),
	language VARCHAR(20),
	copyrightInfo VARCHAR(255)
) WITHOUT ROWID;

CREATE TABLE IF NOT EXISTS Book (
	bookId BIGINT PRIMARY KEY,
	bookTitle VARCHAR(25),
	bookAbbrev VARCHAR(10)
) WITHOUT ROWID;

CREATE TABLE IF NOT EXISTS Chapters (
	chapterId BIGINT PRIMARY KEY,
	chapter VARCHAR(5),
	displayName VARCHAR(25)
) WITHOUT ROWID;

CREATE TABLE IF NOT EXISTS Verses (
	verseId BIGINT PRIMARY KEY,
	verseNumber VARCHAR(10),
	verseText VARCHAR(500)
) WITHOUT ROWID;

CREATE TABLE IF NOT EXISTS BibleLink (
	bibleLinkId BIGINT PRIMARY KEY,
	bibleId BIGINT,
	bookId BIGINT,
	chapterId BIGINT,
	verseId BIGINT
) WITHOUT ROWID;

CREATE TABLE IF NOT EXISTS Indexes (
	indexId BIGINT PRIMARY KEY,
	bibleId BIGINT,
	bookId BIGINT,
	chapterId BIGINT,
	verseId BIGINT,
	bibleLinkId BIGINT,
	noteId BIGINT,
	wordId BIGINT,
	referenceId BIGINT,
	materialsId BIGINT
) WITHOUT ROWID;

CREATE TABLE IF NOT EXISTS Notes (
    noteId BIGINT PRIMARY KEY,
    bibleId BIGINT,
    bookId BIGINT,
    chapterId BIGINT,
    verseId BIGINT,
    noteText VARCHAR(2000000)
) WITHOUT ROWID;

CREATE TABLE IF NOT EXISTS Words (
    wordId BIGINT PRIMARY KEY,
    word VARCHAR(100),
    materialsId BIGINT
) WITHOUT ROWID;

CREATE TABLE IF NOT EXISTS Reference (
    referenceId BIGINT PRIMARY KEY,
    citation VARCHAR(15),
    text VARCHAR(255),
    link VARCHAR(10),
    bookId BIGINT,
    chapterId BIGINT,
    verseNumber BIGINT,
    wordId BIGINT
) WITHOUT ROWID;

CREATE TABLE IF NOT EXISTS Materials (
    materialsId BIGINT PRIMARY KEY,
    name VARCHAR(50)
) WITHOUT ROWID;