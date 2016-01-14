//
//  TouchViewDelegate.h
//  Carat
//
//  Created by Jarno Petteri Laitinen on 12/10/15.
//  Copyright © 2015 University of Helsinki. All rights reserved.
//
#import <UIKit/UIKit.h>

#ifndef TouchViewDelegate_h
#define TouchViewDelegate_h


@protocol TouchViewDelegate
-(void) pressStart:(id) sender;
-(void) pressEnd:(id)sender;
@end

#endif /* TouchViewDelegate_h */
