package com.appiancs.plugins.pdfutilities.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the ParameterValidation utility class.
 */
class ParameterValidationTest {

  private static final long VALID_ID = 100L;
  private static final long INVALID_ID = -5L;
  private static final String PARAM_NAME = "TestParameter";

  @Test
  void checkIsPositiveWithValidNumberDoesNotThrow() {
    assertDoesNotThrow(
      () -> ParameterValidation.checkIsPositive(VALID_ID, PARAM_NAME),
      "A valid positive number should not cause an exception.");
  }

  @Test
  void checkIsPositiveWithNegativeNumberThrowsException() {
    assertThrows(
      IllegalArgumentException.class,
      () -> ParameterValidation.checkIsPositive(INVALID_ID, PARAM_NAME),
      "A negative number should throw an IllegalArgumentException.");
  }

  @Test
  void checkIsPositiveWithNegativeNumberThrowsExceptionWithCorrectMessage() {
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
      () -> ParameterValidation.checkIsPositive(INVALID_ID, PARAM_NAME));
    assertEquals(
      "Parameter '" + PARAM_NAME + "' must be a positive number.",
      exception.getMessage(),
      "The exception message for a negative number was incorrect.");
  }

  @Test
  void checkIsPositiveWithNullThrowsException() {
    assertThrows(
      IllegalArgumentException.class,
      () -> ParameterValidation.checkIsPositive((Long) null, PARAM_NAME),
      "A null value should throw an IllegalArgumentException.");
  }
}