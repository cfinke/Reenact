//
//  ShareController.swift
//  Reenact
//
//  Created by Christopher Finke on 11/25/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit

class HelpController: ReenactControllerBase {
    // MARK: Properties
    
    override func viewDidLoad() {
        super.viewDidLoad()
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    // MARK: Actions
    func closeHelp(sender: UIButton!) {
        self.performSegueWithIdentifier("helpToIntro", sender: self)
    }
    
    // MARK: Delegates
    
}

