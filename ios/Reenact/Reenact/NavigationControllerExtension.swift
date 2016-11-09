//
//  NavigationControllerExtension.swift
//  Reenact
//
//  Created by Christopher Finke on 11/28/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit
import Foundation

extension UINavigationController {
    public override func shouldAutorotate() -> Bool {
        var shouldAutorotate = false
        
        if visibleViewController != nil {
            shouldAutorotate = visibleViewController!.shouldAutorotate()
        }
        
        return shouldAutorotate
    }
    
    override public func supportedInterfaceOrientations() -> UIInterfaceOrientationMask {
        return visibleViewController!.supportedInterfaceOrientations()
    }
}
