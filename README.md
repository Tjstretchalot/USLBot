# USLBot

The USLBot helps users that are a part of the universal-scammer-list coalition identify malintentioned users and reduce the amount of harm they can do. Any user on a participating subreddit who is banned for scamming or suspected scamming is banned on all participating subreddits. 

## How to commit

The first stage to assisting with the development of the USLBot is getting a local version that is compilable, and being able to run tests.

### Dependencies

- Latest [jReddit](https://github.com/tjstretchalot/jreddit)
- Latest [Summonable-Bot](https://github.com/tjstretchalot/summonable-bot)
- log4j-2.0 (api and core)
- mysql-connector-5.1.19 
- json-simple-1.1.1
- JUnit 4

It is suggested that you clone jReddit and Summonable-Bot into the same workspace that you work on the USLBot for and add them as project dependencies, so it's easier to pull the latest changes. Typically you do not need to add JUnit 4 at this step, since Eclipse will prompt you to add it to the workspace when you try to run a test.

### Setting up testing environment

In order to run the tests, you must have some basic configuration prepared. Create the following file structure at the same level as the src/ folder:

    tests/
      database.properties
      user.properties
      
Under user.properties you can use the following:

    username=Test
    password=test
    appClientID=test
    appClientSecret=test

It is not important what information you put in this file, unless the test you are running specifies otherwise (unusual).
    
Under database.properties it will have the following format:

    username=usl
    password=my-secure-password
    url=jdbc:mysql://localhost/databasename

You must run a local mysql instance and create a database that includes the string literal "test". You then need to create an account which has access to that database and put all that information in the database.properties.

At that point, simply run any tests to verify that everything is configured correctly. For example, run me.timothy.tests.database.mysql.MysqlDatabaseTests.