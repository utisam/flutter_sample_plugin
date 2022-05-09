import 'dart:async';

import 'package:flutter/services.dart';

class SamplePlugin {
  static const MethodChannel _methodChannel = MethodChannel('com.example.sample_plugin');
  static const EventChannel _eventChannel = EventChannel('com.example.sample_plugin/sample');

  static Future<String?> get platformVersion async {
    final String? version = await _methodChannel.invokeMethod('getPlatformVersion');
    return version;
  }

  static Stream<int> sampleStream = _eventChannel.receiveBroadcastStream().map((event) => event as int);
}
