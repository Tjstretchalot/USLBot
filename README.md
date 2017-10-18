# USLBot

The USLBot helps users that are a part of the universal-scammer-list coalition identify malintentioned users and reduce the amount of harm they can do. Any user on a participating subreddit who is banned for scamming or suspected scamming is banned on all participating subreddits. 

## Issues

The best way to help this bot is to create issues for any bugs you detect or features you want to see. 

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

### Pull requests

Your pull request will stand a good chance of being accepted if:

- It directly references an issue, though you may create this issue. The issue must describe what was happening before, what happens with this pull request, and why that is better. If the improvement is subjective, it must also include references to either a poll on the appropriate subreddit or modmail. 
- If it is a feature addition, it should include an example of someone who requested this feature or plans for how to tell people about the new feature. This is as easy as a post on a subreddit that you've written out.
- All new functions or classes are documented using JavaDoc
- If it requires any modifications to the database, it must lay out exactly what those changes are and how they should be done. Additionally,
the relevant models must be updated to test that the database fits the new required schema, and all relevant tests must be updated. At least 
one additional test must be added that tests the new column.
- Functions must be titled using lowerCamelCase, classes with UpperCammelCase, variables with lowerCamelCase, etc.

### Example pull request ideas 

For easier pull request ideas I suggest:

- Documentation improvements. Theres almost always an unclear sentence or typo or inconsistency somewhere
- Test improvements. Add a new test for a case that isn't covered or improve an existing test.
- Refactoring (minor!). There's always a better way to structure code. These changes should first be 
suggested in an issue since there may be a good reason why it is the way it is.
- Handle an existing issue. Please comment on the issue saying you are going to work on it!