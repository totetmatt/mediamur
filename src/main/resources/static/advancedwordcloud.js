 class AdvancedTextSearch {
           constructor(elementId) {
               this.elementIdName = elementId;
               this.elementId = document.getElementById(elementId);
               this.graph = {};
               this.counter = {};
               this.isSelected = false;
               this.selectedWord = "";
               this.selectedRelatedWords = [];
           }
           updateList(listWords) {
            var obj = this;
            listWords.forEach(function(word1) {
                obj.updateCounter(word1)

                listWords.forEach(function(word2) {
                    if(word2 !== word1) {
                        obj.updateGraph(word1,word2);
                    }
                });

            });
            this.refreshView()
           }
           updateCounter(newWord) {
                var word = newWord.toLowerCase();
                if(this.counter[word] === undefined) {
                    this.counter[word] = 0;
                }
                this.counter[word]++;
            }

            updateGraph(source,target) {
                if(this.graph[source] === undefined) {
                    this.graph[source] = new Set();
                }
                this.graph[source].add(target)
            }
           refreshView() {
                var self = this
                var maxScore = Math.min(30,Math.max(...Object.values(this.counter)))
                var sortedWords = Object.keys(this.counter).sort();
                var prev = null;
                for(var word in sortedWords) {
                            var w = sortedWords[word]
                            var node = document.getElementById('__'+this.elementIdName+'_'+w);
                            if( node ===  null) {
                                var span = document.createElement("span");
                                span.onclick = function(a) {
                                    if(self.selectedWord == a.target.textContent ) {
                                        self.selectedWord =""
                                        self.isSelected = false
                                        self.onWordClickUnselect(a.target.textContent)
                                        self.selectedRelatedWords = []
                                    } else if(self.selectedWord == "") {
                                        self.selectedWord = a.target.textContent
                                        self.onWordClickSelect(a.target.textContent)
                                        var keepNodes =self.graph[a.target.textContent];
                                        self.isSelected = true;
                                        self.selectedRelatedWords = keepNodes
                                    }

                                }
                                span.id = '__'+this.elementIdName+'_'+w;
                                span.innerHTML = w;
                                span.onmouseover=this.onOver();
                                span.onmouseleave=this.onLeave();
                                var size =  12+  Math.min((20 * (this.counter[w] / maxScore)) + 10,50) * Math.tanh(5*(this.counter[w] / maxScore)/2)
                                span.style.fontSize= size + 'px'
                                if(prev === null) {
                                    this.elementId.prepend(span);
                                } else {
                                    this.elementId.insertBefore(span,document.getElementById('__'+this.elementIdName+'_'+prev).nextSibling);
                                }
                            } else {
                                var size = 12+ ((20 * (this.counter[w] / maxScore)) + 10) * Math.tanh(5*(this.counter[w] / maxScore)/2)
                                node.style.fontSize= size + 'px';
                            }
                            prev = w;
                }
            }
            onOver() {
                  var obj = this;
                  return function(e) {
                        if(obj.selectedWord == "" ) {
                            var keepNodes =obj.graph[e.target.innerHTML];
                            if(keepNodes === undefined) {keepNodes = new Set([])}
                                    var k = Object.keys(obj.counter).sort();
                                    for(var w in k) {
                                            w = k[w]
                                            if(!keepNodes.has(w)   ) {
                                            var node = document.getElementById('__'+obj.elementIdName+'_'+w);
                                            node.style.color ="#CCC";
                                            }
                                    }
                            e.target.style.color="#EE0000";
                         }
                    }
                }
            onLeave() {
                    var obj = this;

                    return function(e){
                        obj.selectedRelatedWords = []
                        if(obj.selectedWord == "" ) {
                            var k = Object.keys(obj.counter).sort();
                            for(var w in k) {
                                            w = k[w]
                                            var node = document.getElementById('__'+obj.elementIdName+'_'+w);
                                            node.style.color ="#000";
                                    }
                            }
                        }
                    }
       }