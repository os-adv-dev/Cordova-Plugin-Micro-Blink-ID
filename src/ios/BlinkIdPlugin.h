//
//  MobileECTPlugin.h
//  OutSystems - Mobility Experts
//
//  Created by Vitor Oliveira on 14/10/15.
//
//

#import <Cordova/CDVPlugin.h>
#import <MicroBlink/MicroBlink.h>

@interface BlinkIdPlugin : CDVPlugin <MBDocumentOverlayViewControllerDelegate>

- (void) readCardId:(CDVInvokedUrlCommand*)command;
- (void) scannDocument:(CDVInvokedUrlCommand*)command;

@end