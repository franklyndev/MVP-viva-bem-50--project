import 'package:flutter/material.dart';

const vivaTeal = Color(0xFF1F6F6A);
const vivaAccent = Color(0xFFE08A2C);
const vivaBg = Color(0xFFF6F3EE);
const vivaDanger = Color(0xFFB42318);
const vivaSuccess = Color(0xFF2F7D4A);

TextTheme _scaledTextTheme() {
  const factor = 1.15;
  final base = Typography.blackMountainView;

  TextStyle? scale(TextStyle? style) {
    final size = style?.fontSize;
    return size == null ? style : style!.copyWith(fontSize: size * factor);
  }

  return base.copyWith(
    displayLarge: scale(base.displayLarge),
    displayMedium: scale(base.displayMedium),
    displaySmall: scale(base.displaySmall),
    headlineLarge: scale(base.headlineLarge),
    headlineMedium: scale(base.headlineMedium),
    headlineSmall: scale(base.headlineSmall),
    titleLarge: scale(base.titleLarge),
    titleMedium: scale(base.titleMedium),
    titleSmall: scale(base.titleSmall),
    bodyLarge: scale(base.bodyLarge),
    bodyMedium: scale(base.bodyMedium),
    bodySmall: scale(base.bodySmall),
    labelLarge: scale(base.labelLarge),
    labelMedium: scale(base.labelMedium),
    labelSmall: scale(base.labelSmall),
  );
}

ThemeData vivaTheme() {
  return ThemeData(
    colorSchemeSeed: vivaTeal,
    scaffoldBackgroundColor: vivaBg,
    useMaterial3: true,
    textTheme: _scaledTextTheme(),
    appBarTheme: const AppBarTheme(
      backgroundColor: Colors.white,
      foregroundColor: vivaTeal,
      elevation: 0.4,
    ),
  );
}
