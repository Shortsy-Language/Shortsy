package syntatic.analysis;

import exceptions.SyntaticException;
import lexical.analysis.Scanner;
import lexical.analysis.Token;
import lexical.analysis.TokenKind;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static lexical.analysis.TokenKind.*;

/**
 * The Syntax analyzer (Parser) takes a string of tokens from the Lexical analyzer (Scanner),
 * and verifies that the string input of tokens can be generated by accordance to the grammar
 * of the source language, returns any syntax error for the source language
 */
public class Parser {
    private final Scanner scanner;
    private Token currentTerminal;
    private static final Logger logger = LogManager.getLogger(Parser.class);

    public Parser(Scanner scanner) {
        System.setProperty("log4j.configurationFile", "src/main/resources/log4j2.properties");
        this.scanner = scanner;
        currentTerminal = scanner.scan();
    }

    public void parseProgram() throws SyntaticException {
        parseBlock();
        if (currentTerminal.kind != EOT) {
            logger.error("Syntax error: Tokens found after end of program");
            throw new SyntaticException("Tokens found after the end of the program");
        }
    }

    /**
     * Check if declaration of a block is valid
     */
    private void parseBlock() throws SyntaticException {
        accept(DECLARE);
        accept(LEFT_BRACE);
        parseDeclarations();
        parseStatements();
        accept(RIGHT_BRACE);
    }


    private void parseDeclarations() throws SyntaticException {
        while (currentTerminal.kind == EOT ||
                currentTerminal.kind == FUNC)
            parseOneDeclaration();
    }


    private void parseOneDeclaration() throws SyntaticException {
        switch (currentTerminal.kind) {
            case INTEGER:
                accept(INTEGER);
                accept(DECLARE_VAR_TYPE);
                accept(IDENTIFIER);
                accept(SEMICOLON);
                break;
            case BOOLEAN:
                accept(BOOLEAN);
                accept(DECLARE_VAR_TYPE);
                accept(IDENTIFIER);
                accept(SEMICOLON);
                break;

            case FUNC:
                accept(FUNC);
                accept(IDENTIFIER);
                accept(LEFT_PARAM);

                if (currentTerminal.kind == IDENTIFIER)
                    parseIdList();

                accept(RIGHT_PARAM);
                parseBlock();
                accept(RETURN);
                parseExpression();
                accept(SEMICOLON);
                break;

            default:
                logger.error("Syntax error: Variable or function expected");
                throw new SyntaticException("Variable or function expected when parsing single declaration");
        }
    }

    private void parseIdList() throws SyntaticException {
        accept(IDENTIFIER);

        while (currentTerminal.kind == COMMA) {
            accept(COMMA);
            accept(IDENTIFIER);
        }
    }

    private void parseStatements() throws SyntaticException {
        while (currentTerminal.kind == IDENTIFIER ||
                currentTerminal.kind == OPERATOR ||
                currentTerminal.kind == INTEGER ||
                currentTerminal.kind == BOOLEAN ||
                currentTerminal.kind == LEFT_PARAM ||
                currentTerminal.kind == IF ||
                currentTerminal.kind == WHILE ||
                currentTerminal.kind == INPUT ||
                currentTerminal.kind == OUTPUT)
            parseOneStatement();
    }

    private void parseOneStatement() throws SyntaticException {
        switch (currentTerminal.kind) {
            case IDENTIFIER:
                parseExpression();
                if (currentTerminal.kind == SEMICOLON) {
                    accept(SEMICOLON);
                    break;
                } else if (currentTerminal.kind == ASSIGNMENT_OPERATOR) {
                    accept(ASSIGNMENT_OPERATOR);
                    if (currentTerminal.kind == BOOLEAN_LITERAL) {
                        accept(BOOLEAN_LITERAL);
                    } else if (currentTerminal.kind == INTEGER_LITERAL) {
                        accept(INTEGER_LITERAL);
                    }
                    accept(SEMICOLON);
                    break;
                }
            case OPERATOR:
            case LEFT_PARAM:
                parseExpression();
                accept(SEMICOLON);
                break;
            case INTEGER:
            case BOOLEAN:
                parseExpression();
                accept(DECLARE_VAR_TYPE);
                accept(IDENTIFIER);
                if (currentTerminal.kind == ASSIGNMENT_OPERATOR) {
                    accept(ASSIGNMENT_OPERATOR);
                    if (currentTerminal.kind == BOOLEAN_LITERAL) {
                        accept(BOOLEAN_LITERAL);
                    } else if (currentTerminal.kind == INTEGER_LITERAL) {
                        accept(INTEGER_LITERAL);
                    }
                }
                accept(SEMICOLON);
                break;

            case IF:
                accept(IF);
                parseExpression();
                parseStatements();
                if (currentTerminal.kind == ELSE) {
                    accept(ELSE);
                    parseStatements();
                }

                break;

            case WHILE:
                accept(WHILE);
                parseExpression();
                accept(DO);
                parseStatements();
                accept(SEMICOLON);
                break;

            case INPUT:
                accept(INPUT);
                parseExpression();
                accept(SEMICOLON);
                break;
            case OUTPUT:
                accept(OUTPUT);
                parseExpression();
                accept(SEMICOLON);
                break;

            default:
                logger.error("Syntax error: Error found in statement");
                throw new SyntaticException(String.format("Error found in single statement: statement does not match syntax expected" +
                        "Current terminal kind found [%s]", currentTerminal.kind.getSpelling()));
        }
    }

    private void parseExpression() throws SyntaticException {
        parsePrimary();
        while (currentTerminal.kind == OPERATOR) {
            accept(OPERATOR);
            parsePrimary();
        }
        while(currentTerminal.kind == COMMA){
            accept(COMMA);
            parsePrimary();
        }
    }

    private void parsePrimary() throws SyntaticException {
        switch (currentTerminal.kind) {
            case IDENTIFIER:
                accept(IDENTIFIER);

                if (currentTerminal.kind == LEFT_PARAM) {
                    accept(LEFT_PARAM);

                    if (currentTerminal.kind == IDENTIFIER ||
                            currentTerminal.kind == INTEGER ||
                            currentTerminal.kind == BOOLEAN ||
                            currentTerminal.kind == OPERATOR ||
                            currentTerminal.kind == LEFT_PARAM)
                        parseExpressionList();


                    accept(RIGHT_PARAM);
                }
                break;

            case INTEGER:
                accept(INTEGER);
                break;
            case BOOLEAN:
                accept(BOOLEAN);
                break;

            case OPERATOR:
                accept(OPERATOR);
                parsePrimary();
                break;

            case LEFT_PARAM:
                accept(LEFT_PARAM);
                parseExpression();
                accept(RIGHT_PARAM);
                break;
            case INTEGER_LITERAL:
            case BOOLEAN_LITERAL:
                if(currentTerminal.kind==INTEGER_LITERAL){
                    accept(INTEGER_LITERAL);
                    break;
                }
                else{
                    accept(BOOLEAN_LITERAL);
                    break;
                }
            default:
                logger.error("Syntax error: Error occurred in while primary parsing");
                throw new SyntaticException(String.format("Error in parsing main statement: statement does not match syntax expected" +
                        "Current terminal kind found [%s]", currentTerminal.kind.getSpelling()));
        }
    }


    private void parseExpressionList() throws SyntaticException {
        parseExpression();
        while (currentTerminal.kind == COMMA) {
            accept(COMMA);
            parseExpression();
        }
    }

    /**
     * accept checks if the token given matches the token expected,
     * if not reports a syntactic error
     *
     * @param expected token
     */
    private void accept(TokenKind expected) throws SyntaticException {
        if (currentTerminal.kind == expected) {
            currentTerminal = scanner.scan();
        } else {
            logger.error(String.format("Syntax error: Expected token of kind [%s]", expected));
            throw new SyntaticException(String.format("Syntax error: Expected token of kind [%s]", expected));
        }
    }
}
