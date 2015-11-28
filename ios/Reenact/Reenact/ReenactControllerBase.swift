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
    let buttonContainerSize: Int = 100
    let smallButtonSize: Int = 60

    override func viewDidLoad() {
        super.viewDidLoad();
        
        // Hide the navigation bar. We'll take care of navigation elements ourself.
        self.navigationController?.setNavigationBarHidden(true, animated: false)
    }
}