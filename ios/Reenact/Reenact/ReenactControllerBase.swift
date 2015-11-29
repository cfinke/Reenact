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
    let buttonContainerSize: CGFloat = 100.0
    let smallButtonSize: CGFloat = 60.0

    override func viewDidLoad() {
        super.viewDidLoad();
        
        // Hide the navigation bar. We'll take care of navigation elements ourself.
        self.navigationController?.setNavigationBarHidden(true, animated: false)
    }
    
    override func viewWillTransitionToSize(size: CGSize,
        withTransitionCoordinator coordinator: UIViewControllerTransitionCoordinator) {
            super.viewWillTransitionToSize(size, withTransitionCoordinator: coordinator);
            
            print("Rotating")
            print(size)
        
        buildLayout(size)
    }
    
    func buildLayout(size: CGSize){
        view.subviews.forEach({ $0.removeFromSuperview() })
    }
    
    func alert(message: String) {
        let alert = UIAlertController(title: "Uh-oh", message: message, preferredStyle: UIAlertControllerStyle.Alert)
        alert.addAction(UIAlertAction(title: "I understand", style: UIAlertActionStyle.Default, handler: nil))
        self.presentViewController(alert, animated: true, completion: nil)
    }
}