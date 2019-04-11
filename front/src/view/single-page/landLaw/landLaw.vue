<template>
  <div>
    <Row>
      <Col>
        <Card>
          <Form :label-width="80">
            <FormItem label="進度">
              <Progress :percent="progress">{{`${offset+1}/${count+1}`}}</Progress>
            </FormItem>
            <FormItem label="內容">
              <Input v-model="study.content" placeholder="..." type="textarea" :rows="4"/>
            </FormItem>

            <FormItem>
              <Button
                type="primary"
                style="margin-left: 8px"
                @click="pre"
                :disabled="offset===0"
              >&lt;</Button>
              <Button type="primary" style="margin-left: 8px" @click="speak">朗誦</Button>
              <Button type="primary" style="margin-left: 8px" @click="show_add_keyword_dlg">增加圖片</Button>
              <Button type="primary" style="margin-left: 8px" @click="clear_keywords">清除圖片</Button>
              <Button type="primary" style="margin-left: 8px" @click="upsert">儲存</Button>
              <Button type="primary" style="margin-left: 8px" @click="del">刪除</Button>
              <Button
                type="primary"
                style="margin-left: 8px"
                @click="next"
                :disabled="offset>=count"
              >&gt;</Button>
            </FormItem>
          </Form>
        </Card>
        <Modal v-model="add_keyword_modal" title="增加圖片" @on-ok="onAddKeyword">
          <Form ref="form_add_keyword" :model="add_keyword" inline :closable="false">
            <FormItem label="關鍵字">
              <Input type="text" v-model="add_keyword.key" placeholder="關鍵字..."></Input>
            </FormItem>
            <FormItem label="url">
              <Input type="url" v-model="add_keyword.url" placeholder="http://..."></Input>
            </FormItem>
            <FormItem label="上傳圖檔">
              <Upload action="/photo">
                <Button icon="ios-cloud-upload-outline">上傳圖檔</Button>
              </Upload>
            </FormItem>
          </Form>
        </Modal>
      </Col>
    </Row>
    <Row>
      <Col v-if="study.keywords.length">
        <Carousel v-model="keyword_idx" :autoplay="true">
          <CarouselItem v-for="keyword in study.keywords" :key="keyword.key">
            <Card :title="keyword.key">
              <img :src="keyword.url">
            </Card>
          </CarouselItem>
        </Carousel>
      </Col>
    </Row>
  </div>
</template>
<style scoped>
</style>
<script>
import {
  getLandLaw,
  getLandLawCount,
  upsertLandLaw,
  delLandLaw
} from "@/api/data";
export default {
  name: "landLaw",
  mounted() {
    this.fetchLawCount();
    this.fetchLaw();
  },
  data() {
    return {
      offset: 0,
      count: 0,
      add_keyword_modal: false,
      add_keyword: {
        key: "test",
        url: "http://www.google.com/"
      },
      keyword_idx: 0,
      study: {
        _id: "000000000000000000000000",
        content: "",
        keywords: []
      }
    };
  },
  computed: {
    progress() {
      return Math.ceil((this.offset * 100) / (this.count + 1));
    }
  },
  methods: {
    fetchLaw() {
      getLandLaw({ offset: this.offset })
        .then(resp => {
          const data = resp.data;
          this.study._id = data._id;
          this.study.content = data.content;
          this.study.keywords.splice(0, this.study.keywords.length);
          for (let key of data.keywords) {
            this.study.keywords.push(key);
          }
        })
        .catch(err => {
          alert(err);
        });
    },
    fetchLawCount() {
      getLandLawCount()
        .then(resp => {
          const data = resp.data;
          this.count = data.count;
        })
        .catch(err => {
          alert(err);
        });
    },
    pre() {
      if (this.offset) this.offset -= 1;

      this.fetchLaw();
    },
    next() {
      this.offset += 1;
      this.fetchLaw();
    },
    show_add_keyword_dlg() {
      this.add_keyword_modal = true;
    },
    clear_keywords() {
      this.study.keywords.splice(0, this.study.keywords.length);
    },
    onAddKeyword() {
      let new_keyword = Object.assign({}, this.add_keyword);
      this.add_keyword.key = ""
      this.add_keyword.url = ""
      this.study.keywords.push(new_keyword);
    },
    upsert() {
      upsertLandLaw({ study: this.study })
        .then(resp => {
          this.$Message.info("成功");
        })
        .catch(err => {
          alert(err);
        });
    },
    del() {
      delLandLaw(this.study._id)
        .then(resp => {
          const data = resp.data;
          this.count -= data.count;
          if (this.offset >= this.count) this.offset = this.count - 1;

          this.fetchLaw();
        })
        .catch(err => alert(err));
    },
    speak() {
      if ("speechSynthesis" in window) {
        let synth = window.speechSynthesis;
        let voices = synth.getVoices();
        let utterThis = new SpeechSynthesisUtterance(this.study.content);

        let twVoice = voices.filter(v => {
          return v.lang === "zh-TW";
        });

        utterThis.voice = twVoice[0];

        utterThis.pitch = 1;
        utterThis.rate = 1;
        synth.speak(utterThis);
      } else {
        alert("speechSynthesis is not supported!");
      }
    }
  }
};
</script>
