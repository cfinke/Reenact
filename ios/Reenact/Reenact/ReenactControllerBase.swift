//
//  ReenactControllerBase.swift
//  Reenact
//
//  Created by Christopher Finke on 11/28/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit
import Foundation

class ReenactControllerBase: UIViewController {
    let buttonContainerSize: CGFloat = 80.0
    let smallButtonSize: CGFloat = 50.0
    let largeButtonSize: CGFloat = 65.0
    
    let screenshotMode = false
    let screenshotModeOrientation = "portrait"
    
    override func viewDidLoad() {
        super.viewDidLoad();
        
        // Hide the navigation bar. We'll take care of navigation elements ourself.
        self.navigationController?.setNavigationBarHidden(true, animated: false)
        
        let leftSwipe = UISwipeGestureRecognizer(target: self, action: Selector("handleSwipes:"))
        let rightSwipe = UISwipeGestureRecognizer(target: self, action: Selector("handleSwipes:"))
        
        leftSwipe.direction = .Left
        rightSwipe.direction = .Right
        
        view.addGestureRecognizer(leftSwipe)
        view.addGestureRecognizer(rightSwipe)
    }
    
    override func shouldAutorotate() -> Bool {
        return true
    }
    
    override func supportedInterfaceOrientations() -> UIInterfaceOrientationMask {
        return [
            UIInterfaceOrientationMask.Portrait,
            UIInterfaceOrientationMask.PortraitUpsideDown,
            UIInterfaceOrientationMask.LandscapeRight,
            UIInterfaceOrientationMask.LandscapeLeft,
            UIInterfaceOrientationMask.Landscape
        ]
    }
    
    override func viewWillTransitionToSize(size: CGSize,
        withTransitionCoordinator coordinator: UIViewControllerTransitionCoordinator) {
        super.viewWillTransitionToSize(size, withTransitionCoordinator: coordinator);
        buildLayout(size)
    }
    
    override func prefersStatusBarHidden() -> Bool {
        return true
    }
    
    func buildLayout(size: CGSize){
        view.subviews.forEach({ $0.removeFromSuperview() })
    }
    
    func textSize(text: String, font: UIFont) -> CGSize {
        return (text as NSString).sizeWithAttributes([NSFontAttributeName: font])
    }
    
    /**
     * @see http://stackoverflow.com/a/30841417
     */
    func backgroundThread(delay: Double = 0.0, background: (() -> Void)? = nil, completion: (() -> Void)? = nil) {
        dispatch_async(dispatch_get_global_queue(Int(QOS_CLASS_USER_INITIATED.rawValue), 0)) {
            if(background != nil){ background!(); }
            
            let popTime = dispatch_time(DISPATCH_TIME_NOW, Int64(delay * Double(NSEC_PER_SEC)))
            dispatch_after(popTime, dispatch_get_main_queue()) {
                if(completion != nil){ completion!(); }
            }
        }
    }
    
    func handleSwipes(sender: UISwipeGestureRecognizer) {
        if (sender.direction == .Right) {
            swipeRight()
        }
        else if (sender.direction == .Left) {
            swipeLeft()
        }
    }
    
    func swipeRight(){}
    func swipeLeft(){}
}